package com.FlixGrid.contentservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "movies")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private Genre genre;

    private String director;
    private String cast;

    private int releaseYear;
    private double rating;

    private String thumbnailUrl;
    private int durationMinutes;

    // S3 key for video file
    //You upload a movie. It goes to S3. The path is saved as the videoKey eg raw-videos/inception_1080p.mp4
    private String videoKey;

    // HLS master playlist URL for streaming
    // it send the playlist of the whole video file which is converted in chunks
    // the chunks playlist url is sent to the player
    private String hlsUrl;

    // video is uploaded , failed etc
    @Enumerated(EnumType.STRING)
    private VideoStatus videoStatus;

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;


}
