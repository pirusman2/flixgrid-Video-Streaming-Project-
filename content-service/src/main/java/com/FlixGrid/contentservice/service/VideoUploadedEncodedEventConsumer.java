package com.FlixGrid.contentservice.service;

import com.FlixGrid.contentservice.model.VideoStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoUploadedEncodedEventConsumer {

    private final ContentService contentService;

    @KafkaListener(
            topics = "video.uploaded"
    )
    public void consumerVideoUploadedEvent(
            @Payload Map<String , Object> payload){

        String movieId = payload.get("movieId").toString();
        String videoKey = payload.get("videoKey").toString();

        log.info("Video Uploaded for movie: {} and key: {}", movieId, videoKey);
        contentService.updateVideoKey(movieId, videoKey);
    }


    @KafkaListener(
            topics = "video.encoded")
    public void consumeVideoEncodedEvent(
            @Payload Map<String, Object> payload){
        String movieId = payload.get("movieId").toString();
        String hlsUrl = payload.get("hlsUrl").toString();
        boolean success = (Boolean) payload.get("success");

        if (success){
            contentService.updateHlsUrl(movieId, hlsUrl);
        }
        else {
            String error = payload.get("errorMessage").toString();
            contentService.updateVideoStatus(movieId, VideoStatus.FAILED);
        }

    }


}
