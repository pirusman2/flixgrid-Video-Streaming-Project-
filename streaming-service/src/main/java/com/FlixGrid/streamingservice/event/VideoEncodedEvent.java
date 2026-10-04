package com.FlixGrid.streamingservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * Consumed from Kafka topic which is video.encoded
 * Which will be published by EncodingService when FFmpeg processing is done
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoEncodedEvent {

    private String movieId;
    private String hlsUrl;
    private String masterPlaylist;
    private boolean success;
    private String errorMessage;
}
