package com.FlixGrid.contentservice.dto;

import com.FlixGrid.contentservice.model.VideoStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse {

    private String id;
    private String title;

    private String description;

    private String genre;

    private String director;

    private String cast;

    private String releaseYear;

    private double rating;

    private String thumbnailUrl;

    private int durationMinutes;

    private String videoKey;

    private String hlsUrl;

    private VideoStatus videoStatus;

    private LocalDateTime createdAt;

}
