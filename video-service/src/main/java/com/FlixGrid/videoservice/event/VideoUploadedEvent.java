package com.FlixGrid.videoservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event publish to Kafka when video is uploaded to S3.
 * And Encoding service will consume this event to Start the FFmpeg
 *
 * TOPIC: video.uploaded
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoUploadedEvent {

    private String movieId;
    private String videoKey;
    private String bucketName;
    private String originalFilename;
    private Long fileSizeBytes;
}
