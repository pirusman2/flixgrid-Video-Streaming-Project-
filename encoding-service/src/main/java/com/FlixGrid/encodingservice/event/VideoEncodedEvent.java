package com.FlixGrid.encodingservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoEncodedEvent {
    private String movieId;        // "Which movie?" -> "movie-101"
    private String hlsUrl;         // "What is the web link to watch it?" -> "https://.../master.m3u8"
    private String masterPlaylist; // "Where is the file in S3?" -> "encoded/movie-101/master.m3u8"
    private boolean success;       // "Did it work?" -> true
    private String errorMessage;   // "Did it fail?" -> null
}