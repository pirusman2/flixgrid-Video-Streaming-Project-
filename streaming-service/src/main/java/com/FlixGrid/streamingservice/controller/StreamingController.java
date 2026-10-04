package com.FlixGrid.streamingservice.controller;

import com.FlixGrid.streamingservice.dto.StreamingResponse;
import com.FlixGrid.streamingservice.services.StreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stream")
@Slf4j
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class StreamingController {

    private final StreamingService streamingService;

    private final RedisTemplate<String, String> redisTemplate; //RedisTemplate is the tool for talking to Redis

    private static final String MASTER_PLAYLIST_KEY_PREFIX = "streaming:playlist:";

    /**
     * Get streaming url for a movie
     * Returns a presigned HLS master playlist URl
     *
     * GET /api/v1/stream{movieId}
     */

    @GetMapping("/{movieId}")
    public ResponseEntity<StreamingResponse> getStreamingUrl(
            @PathVariable String movieId){

        log.info("Streaming request for movie: {}", movieId);

        // Get master playlist key from redis
        String playlistKey = redisTemplate.opsForValue().
                get(MASTER_PLAYLIST_KEY_PREFIX + movieId);

        if (playlistKey == null){
            return ResponseEntity.notFound().build();
        }

           StreamingResponse response = streamingService.getStreamingUrl(movieId,playlistKey);

        return ResponseEntity.ok(response);
    }


    /**
     *Server signed m3u8 playlist content
     * Called by HLS player for each quality
     */
    @GetMapping("/{movieId}/playlist")
    public ResponseEntity<String> getSignedPlaylist(
            @PathVariable String movieId,
            @RequestParam String path){

        String signedPlaylist = streamingService.getSignedPlaylist(movieId,path);
        return ResponseEntity.ok()
                .header("Content-Type","application/x-mpegURL") // this is an Video Playlist
                .body(signedPlaylist);
    }






}
