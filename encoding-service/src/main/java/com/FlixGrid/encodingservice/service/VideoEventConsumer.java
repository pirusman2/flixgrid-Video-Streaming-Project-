package com.FlixGrid.encodingservice.service;

import com.FlixGrid.encodingservice.event.VideoEncodedEvent;
import com.FlixGrid.encodingservice.event.VideoUploadedEvent;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoEventConsumer {

    private final EncodingService encodingService;

    /**
     * Listens to video.uploaded Kafka topic
     * Triggered when video service uploads a raw video to s3
     *
     * FLOW:
     *
     * VideoService -> S3 Upload -> Kafka (video.uploaded)
     *                           -> This Consumer
     *                           -> EncodingService -> FFmpeg ->S3
     *                           -> Kafka (video.encoded)
     */


    // listens to videoservice -> videouploaded event so to encode it
    @KafkaListener(
            topics = "video.uploaded",
            groupId = "encoding-service-group"
    )

    public void consumeVideoUploadedEvent(VideoUploadedEvent event){

        log.warn("Consumed VideoUploadedEvent for movie: {}, file: {}",
                event.getMovieId(),event.getOriginalFilename());

        try {
            encodingService.encodeVideo(event);
        }
        catch (Exception e) {
            log.error("Failed to process encoding for movie: {} - {}",
            event.getMovieId(),e.getMessage());
        }

    }

}
