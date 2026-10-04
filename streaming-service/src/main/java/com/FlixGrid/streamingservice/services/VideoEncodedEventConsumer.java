package com.FlixGrid.streamingservice.services;

import com.FlixGrid.streamingservice.event.VideoEncodedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoEncodedEventConsumer {

    private final RedisTemplate<String,String> redisTemplate;

    private static final String MASTER_PLAYLIST_KEY_PREFIX = "streaming:playlist:";

    /**
     * this class listens to kafka topic: video.encoded
     * Stores master playlist key in Redis when encoding is completed
     * This allows the StreamingService to quickly find the playlist key by movieId
     */

    @KafkaListener(
            topics = "video.encoded",  // will listen to this kafka topic
            groupId = "streaming-service-group"
    )

    public void consumeVideoEncodedEvent(VideoEncodedEvent event){

        log.info("Received video encoded event for movie:{} success: {} ",
                event.getMovieId(),event.isSuccess());

        if (event.isSuccess()){
            // then we will store master playlist key in Redis
        String cacheKey = MASTER_PLAYLIST_KEY_PREFIX + event.getMovieId();

        redisTemplate.opsForValue().set(cacheKey, event.getMasterPlaylist());
        log.info("The master playlist key has been stored in Redis for movie: {}",event.getMovieId());

        }
        else {
            log.error("Encoding failed for movie: {} - {}" ,
                    event.getMovieId(),event.getErrorMessage());
        }

    }
}
