package com.FlixGrid.contentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

// when user try to add movie
public class MovieRequest {

    @NotBlank(message = "Title is Required")
    private String title;

    private String description;

    @NotNull(message = "Genre is Required")
    private String genre;

    private String director;

    private String cast;

    private String releaseYear;

    private double rating;

    private String thumbnailUrl;

    private int durationMinutes;
}
