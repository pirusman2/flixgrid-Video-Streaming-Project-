package com.FlixGrid.videoservice.service;

import com.FlixGrid.videoservice.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoService {

    private final S3Client s3Client;
    private final KafkaTemplate<String, VideoUploadedEvent> kafkaTemplate;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private static final String VIDEO_UPLOADED_TOPIC = "video.uploaded";

    /**
     * Upload video to AWS s3 and publish VideoUploadedEvent to Kafka
     *
     * FLOW:
     * 1. Receive multipart video file
     * 2. Generate unique S3 key
     * 3. Upload to S3
     * 4. Publish VideoUploadedEvent to Kafka
     * 5. Encoding Service picks up and start FFmpeg
     */


    public String uploadVideo(String movieId, MultipartFile file) throws IOException {
        log.info("Starting video upload for movie: {} file: {} ", movieId, file.getOriginalFilename());

        //Generate unique S3 key for raw video
        // format: raw/movieId/uuid_filename

        String videoKey = "raw/" +movieId+ "/" + UUID.randomUUID() +
                "_" + file.getOriginalFilename();

        // now we need to build the request
        // request to send the video file to S3

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(videoKey)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        // now lets send it to S3
        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        log.info("Video is uploaded to S3 successfully key: {} ", videoKey);

        // if video is uploaded to S3
        // then publish event to kafka
        // then encoding service will consume this event and start FFmpeg processing


        VideoUploadedEvent event = new VideoUploadedEvent(
                movieId,
                videoKey,
                bucketName,
                file.getOriginalFilename(),
                file.getSize()
        );

        kafkaTemplate.send(VIDEO_UPLOADED_TOPIC,movieId,event);
        log.info("VideoUploadedEvent published successfully for movie: {} ", movieId);

        return videoKey;
    }
}
