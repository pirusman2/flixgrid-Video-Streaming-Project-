package com.FlixGrid.encodingservice.service;

import com.FlixGrid.encodingservice.event.VideoEncodedEvent;
import com.FlixGrid.encodingservice.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EncodingService {

    private final S3Client s3Client;
    private final KafkaTemplate<String, VideoEncodedEvent> kafkaTemplate;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("${encoding.base-path}")
    private String basePath;

    private static final String VIDEO_ENCODED_TOPIC = "video.encoded";

    // videos qualities to encode
    // format: resolution, bitrate, height
    // bitrate -> how much data is processed per second
    // higher bitrate better visual quality and vise versa(blur image quality)

    private static final List<int[]> VIDEO_QUALITIES = Arrays.asList(
            new int[]{1920,5000,1080},  // 1080p , 5000 kbps
            new int[]{1280,2800,720},  // -720p, 2800kbps
            new int[]{854,1200,480},   // -480p, 1200kbps
            new int[]{640,800,360}     // -360, 800kbps
    );

    // so our videoservice uploaded the movie/video to S3
    // now we need to encode it

    /**
     * so main encoding pipeline(flow) is below
     * 1. download raw video from s3 which was uploaded in video service to s3
     * 2. encode to multiple qualities using FFmpeg
     * 3. generate HLS playlist (.m3u8) for each quality
     * 4. create master playlist(one file containing links to other qualities playlist )
     * 5. upload all encoded files back to s3
     * 6. publish VideoEncodedEvent to Kafka
     */
    public void encodeVideo(VideoUploadedEvent event) {
        log.info("Starting encoding platform for movie: {}", event.getMovieId());

        // for step 1 we need to create directories and multiple directories in our local system
        // to save the ffmpeg processed chunks
        // create a unique path for a movie or video
        String jobPath = basePath + "/" + event.getMovieId();

        try{
            // create temp directories
            Files.createDirectories(Paths.get(jobPath));
            Files.createDirectories(Paths.get(jobPath + "/encoded")); // for encoded files

            // step1: Download raw video from S3
            String localVideoPath = jobPath + "/raw_video.mp4"; // path for saving the raw downloaded video from S3

            downloadFromS3(event.getVideoKey(),localVideoPath);  // method telling s3 to find specific movie and saving it in local
            log.info("Raw video downloaded to: {}", localVideoPath);

            // now step 2 and 3 -> Encode to multiple qualities and generate HLS
            // for loop is for running FFmpeg 4 times to generate 4 qualities defined above
            for (int[] qualities: VIDEO_QUALITIES){
                int width = qualities[0];
                int bitrate = qualities[1];
                int height = qualities[2];

                // now we will define our quality directory
                String qualityDir = jobPath + "/encoded/" + height + "p";
                Files.createDirectories(Paths.get(qualityDir)); // specific directory for specific quality

                // sourcepath(raw file downloaded) , where should this quality saved, how many pixels should this version be
                encodeToHLS(localVideoPath,qualityDir, width, height, bitrate);
                log.info("Encoded :{}p successfully ",height);

            }

            // step:4 generate master playlist
            String masterPlaylistPath = jobPath + "/encoded/master.m3u8";
            generateMasterPlaylist(masterPlaylistPath);

            log.info("Master playlist generated");

            // step:5 Upload all resources files back to S3
            String encodedPrefix = "encoded/" + event.getMovieId() + "/";  // in encoded specific path for each movie
            uploadEncodedFilesToS3(jobPath + "/encoded", encodedPrefix);
            log.info("All encoded files uploaded to S3");


            // Step:6  Publish VideoEncodingEvent
            String masterPlaylist = encodedPrefix + "master.m3u8";   // encoded/movie-101/master.m3u8

            // https://my-netflix-bucket.s3.amazonaws.com/encoded/movie-101/master.m3u8
            String hlsUrl = "https://" +bucketName + ".s3.amazonaws.com/" + masterPlaylist;

            VideoEncodedEvent encodedEvent = new VideoEncodedEvent(
                    event.getMovieId(),
                    hlsUrl,
                    masterPlaylist,
                    true,
                    null);

            kafkaTemplate.send(VIDEO_ENCODED_TOPIC, event.getMovieId(), encodedEvent);
            log.info("VideoEncodedEvent publish for movie: {}", event.getMovieId());



        }
        catch (Exception e){

            log.error("Encoding failed for movie: {} - {}", event.getMovieId(), e.getMessage());

            // publish failure message
            VideoEncodedEvent failureEvent = new VideoEncodedEvent(
                    event.getMovieId(),
                    null,
                    null,
                    false,
                    e.getMessage());

            kafkaTemplate.send(VIDEO_ENCODED_TOPIC, event.getMovieId(), failureEvent);
        }

        finally {
            // cleanup temp files
            cleanUpTempFiles(jobPath);
        }
    }

    // download file from s3 to local path (step 1)
     private void downloadFromS3(String s3Key, String localPath){
        GetObjectRequest getObjectRequest = GetObjectRequest.builder() // for downloading from S3
                .bucket(bucketName)
                .key(s3Key)
                .build();
        s3Client.getObject(getObjectRequest, Paths.get(localPath)); // downloading from S3 and saving in local
     }

    /**
     * Encode video to hls using FFmpeg
     * -Multiple  .ts chunks/segments files created(10 seconds each)
     * -A m3u8 playlist file  for this quality
     *
     * @param inputPath
     * @param outputDir
     * @param width
     * @param height
     * @param bitrate
     * @throws IOException
     * @throws InterruptedException
     */

     private void encodeToHLS(String inputPath, String outputDir,
                              int width, int height, int bitrate)throws IOException, InterruptedException {

        String playlistPath = outputDir + "/playlist.m3u8";
        String segmentPattern = outputDir + "/segment_%03d.ts";

        // FFmpeg command for HLS encoding

         // FFmpeg Command for HLS encoding
         List<String> command = Arrays.asList(
                 ffmpegPath,                               // 1. Path to the FFmpeg software (e.g., "C:/ffmpeg/ffmpeg.exe")

                 "-i", inputPath,                          // 2. (-i) Input file: Tells FFmpeg where the raw downloaded video is

                 "-vf", "scale=" + width + ":" + height,   // 3. (-vf) Video Filter: Resizes the video to match our quality (e.g., 1920:1080)

                 "-c:v", "libx264",                        // 4. (-c:v) Video Codec: Uses "libx264", the industry standard for compressing video into H.264 format

                 "-profile:v", "high",                     // force H.264 High profile (matches the CODECS string)
                 "-level", "4.0",                          // force H.264 level 4.0
                 "-pix_fmt", "yuv420p",                    // 8-bit 4:2:0, the only format all browsers decode

                 "-b:v", bitrate + "k",                    // 5. (-b:v) Video Bitrate: Sets the visual quality/compression level (e.g., 5000k for high quality)

                 "-c:a", "aac",                            // 6. (-c:a) Audio Codec: Uses "AAC", the industry standard for compressing audio

                 "-b:a", "128k",                           // 7. (-b:a) Audio Bitrate: Sets audio quality to a standard 128 kbps

                 "-hls_time", "10",                        // 8. Chunk length: Chops the video into tiny files that are exactly 10 seconds long

                 "-hls_list_size", "0",                    // 9. Playlist size: "0" means keep ALL chunks in the playlist (Don't delete old ones like a live TV stream would)

                 "-hls_segment_filename", segmentPattern,  // 10. Chunk names: Tells FFmpeg to name the chopped files like "segment_000.ts", "segment_001.ts"

                 "-f", "hls",                              // 11. (-f) Format: Explicitly tells FFmpeg we want the output format to be HLS (Apple's streaming format)

                 playlistPath                              // 12. Final Output: The destination path for the ".m3u8" master playlist file
         );

         // java way to run terminal command
         ProcessBuilder  processBuilder = new ProcessBuilder(command);
         processBuilder.redirectErrorStream(true);
         processBuilder.inheritIO();
         Process process = processBuilder.start();

         int exitCode = process.waitFor(); // java program will wait for ffmpeg command to finish
         if (exitCode != 0) {   // Exit Code 0 means: "I finished successfully with zero errors."
             throw new RuntimeException("FFmpeg encoding failed with exit code: " + exitCode);
         }

     }

    /**
     * Generate master Hls playlist that references all the qualities playlists
     * This is the file that video player downloads first
     */

     private void generateMasterPlaylist(String masterPlaylistPath)throws IOException{

         StringBuilder master = new StringBuilder(); // empty document where playlist content will be
         master.append("#EXTM3U\n");// It tells the video player "Hey, I am an M3U playlist file!"
         master.append("#EXT-X-VERSION:3\n\n");

         //Add each quality to master playlist
         int[][] qualities = {{1920,5000,1080},{1280,2800,720},
                                {854,1200,480},{640,800,360}};


         for (int[] q: qualities){
             int width = q[0];
             int bitrate = q[1];
             int height = q[2];

             master.append("#EXT-X-STREAM-INF:BANDWIDTH=") //here comes info about one quality stream
                     .append(bitrate*1000)
                     .append(",RESOLUTION=").append(width).append("x").append(height)
                     .append(",CODECS=\"avc1.640028,mp4a.40.2\"\n");
             master.append(height).append("p/playlist.m3u8\n\n");
         }
         Files.writeString(Paths.get(masterPlaylistPath),master.toString());
     }

     private void uploadEncodedFilesToS3(String localDir, String s3Prefix)throws IOException{
         File directory = new File(localDir);
         uploadDirectoryToS3(directory,localDir,s3Prefix);
     }

    /**
     *Uploads all the encoded files from local directory back to S3
     */

     private void uploadDirectoryToS3(File dir, String baseDir, String s3Prefix)throws IOException{
         for (File file: dir.listFiles()) {
             if (file.isDirectory()) {
                 uploadDirectoryToS3(file,baseDir,s3Prefix);
             }
             else{

                 String relativePath = file.getAbsolutePath()
                         .substring(baseDir.length() + 1)
                 .replace("\\","/");

                 String s3Key = s3Prefix + relativePath;
                 String contentType = file.getName().endsWith("m3u8")
                         ? "application/x-mpegURL"
                         : "video/MP2T";

                 PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                         .bucket(bucketName)
                         .key(s3Key)
                         .contentType(contentType)
                         .build();

                 s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
                 log.debug("Uploaded {} to S3", s3Key);
             }
         }
     }

    /**
     * Cleans up the temp files from local disk as they are uploaded to S3
     */

        private void cleanUpTempFiles(String jobPath){
            try {
                Path dirPath = Paths.get(jobPath);

                //Checks if this folder actually exists before trying to delete
                if (Files.exists(dirPath)) {

                   // walks through the given folder and every file and folder nested inside it
                    Files.walk(dirPath)

                            //Sorts that list of files/folders in reverse order
                            //you cannot delete a folder while it still has files inside it
                            .sorted(java.util.Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                    log.info("Clean up temp files successfully for job: {}", jobPath);
                }
            }
            catch (IOException e) {

                log.warn("failed to clean up temp files: {}", e.getMessage());
            }
        }

}


