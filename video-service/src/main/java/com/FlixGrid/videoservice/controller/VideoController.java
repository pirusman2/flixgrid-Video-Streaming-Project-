package com.FlixGrid.videoservice.controller;

import com.FlixGrid.videoservice.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/videos")
@Slf4j
public class VideoController {

    // upload video file for a movie
    // Accept multipart file upload

    private final VideoService videoService;

    @PostMapping("/upload/{movieId}")
    public ResponseEntity<String> uploadVideo(
            @PathVariable String movieId,
            @RequestParam("file") MultipartFile file)throws IOException {

        log.info("Video upload request for movie: {} file size: {}",
                movieId, file.getSize() / (1024 * 1024));

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }
        String videoKey = videoService.uploadVideo(movieId,file);
        return ResponseEntity.ok("Video uploaded successfully"
                                    +videoKey+ " - Encoding started Successfully via Kafka ");
    }

}
