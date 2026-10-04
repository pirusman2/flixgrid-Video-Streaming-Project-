package com.FlixGrid.streamingservice.services;

import com.FlixGrid.streamingservice.dto.StreamingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StreamingService {

    /**
     * When a user clicks Play, it gives them a temporary 60-minute ticket for the movies master playlist file.
     * It uses Redis so if 10,000 users click Play on the same movie, we don't bother AWS 10,000 times.
     * When the video player asks for the actual video clips, it reads the playlist file from S3 and attaches a secure ticket to every single video chunk inside it.
     */




    private final S3Client  s3Client; //Used to download and read files from S3
    private final S3Presigner s3Presigner;  //makes presigned links, the temporary "guest pass"
    private final RedisTemplate<String,String> redisTemplate; // to talk to redis

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiry}")
    private long presignedUrlExpiry; // 60 minutes

    //Redis key for caching streaming url
    private static final String STREAMING_URL_CACHE_PREFIX = "streaming:url:";

    /**
     * Get streaming url for a movie
     *
     * FLOW:
     * 1: check redis cache for existing presigned url
     * 2: If cached- Return immediately
     * 3: If not cached generate new presigned url from s3
     * 4: Cache the url in Redis
     * 5: Return streaming url
     *
     * Why we are using presigned url?
     * -S3 bucket is a private storage room -Videos are not publicly accessible
     * -Presigned url gives temporary access( Like for an x minutes)
     * -Prevent unauthorized video downloads
     */

    public StreamingResponse getStreamingUrl(String movieId, String playlistKey) {
        log.info("Getting streaming url for movie:{}",movieId);

        String cacheKey = STREAMING_URL_CACHE_PREFIX + movieId;

        // check redis Cache if it already exists or not
        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);

        if (cachedUrl != null) {
            log.info("Returning cached streaming url for movie:{}",movieId);
            return new StreamingResponse(movieId,cachedUrl,
                    "1080p ,720p, 480p,360p",presignedUrlExpiry);
        }

        // if null then we need to generate new presigned url from S3
        log.info("Generating new presigned url for movie:{}",movieId);
        String presignedUrl = generatePresignedUrl(playlistKey);

        // now its generated so cached in redis for 55 minutes
        //5 minutes less then actual expiry to avoid edge cases

        redisTemplate.opsForValue().set(
                cacheKey,
                presignedUrl,
                55,
                TimeUnit.MINUTES
        );

        log.info("Streaming url generated and cached for movie:{}",movieId);
        return new StreamingResponse(
                movieId,
                presignedUrl,
                "1080p,720p,480p,360p",
                presignedUrlExpiry
        );
    }

    /**
     *This is the key method that makes everything secure
     */

    public String getSignedPlaylist(String movieId, String playlistPath){

        // Get base path for this playlist
        // From "movies/101/720p/index.m3u8", it cuts off index.m3u8
        String basePath = playlistPath.substring(0, playlistPath.lastIndexOf("/")+1);

        //Read m3u8 content from S3
        String m3u8Content = readFromS3(playlistPath);

        // Rewrite that is S3 segment or playlist reference
        String signedContent = rewriteM3u8SignedUrls(
                m3u8Content,basePath);

        return signedContent;
    }

    // to copy things from m3u8 file in s3 to this empty file
    public String rewriteM3u8SignedUrls(String m3u8Content, String basePath){
        StringBuilder rewritten = new StringBuilder(); // create an empty file or page

        for (String line : m3u8Content.split("\n")) {
            String trimmed = line.trim();

            //Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                rewritten.append(line).append("\n");
                continue;
            }

            // else this is a segment or playlist reference
            //Build full s3 key and sign it

            String fullKey = basePath + trimmed;
            String signedUrl = generatePresignedUrl(fullKey);

            rewritten.append(signedUrl).append("\n");
        }
        return rewritten.toString();
    }

    /**
     * Read m3u8 file content from S3
     */

    public String readFromS3(String s3Key){
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        ResponseInputStream<GetObjectResponse> response =
                s3Client.getObject(request);

        return new BufferedReader(new InputStreamReader(response))
                .lines()
                .collect(Collectors.joining("\n"));

    }





    /**
     * Generate a presigned url for S3 object
     * Url expired after configured time
     */

    private String generatePresignedUrl(String key) {

        // to tell s3 which file we want we use  this getObjectRequest
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        //Create a presigned URL for this S3 object, and make it valid for this amount of time.
        // for how much time we want the file which is define in (GetObjectRequest)
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpiry))
                .getObjectRequest(getObjectRequest).build();

        //S3Presigner, take those instructions from above GetObjectRequest(file) and ObjectPresignRequest
        // and CREATE the temporary URL
        return s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }

    /**
     * Invalidate cache streaming url
     * Called when video is re-encoded or updated
     */
    public void invalidateCache(String movieId) {
        String cacheKey = STREAMING_URL_CACHE_PREFIX + movieId;
        redisTemplate.delete(cacheKey);
        log.info("Streaming url cache invalidated from redis for movie:{}",movieId);
    }
}
