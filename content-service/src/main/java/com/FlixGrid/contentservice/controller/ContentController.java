package com.FlixGrid.contentservice.controller;

import com.FlixGrid.contentservice.dto.MovieRequest;
import com.FlixGrid.contentservice.dto.MovieResponse;
import com.FlixGrid.contentservice.model.Genre;
import com.FlixGrid.contentservice.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/movies")
@Slf4j
public class ContentController {

    private final ContentService contentService;

    // Add new movie to catalog
    @PostMapping
    public ResponseEntity<MovieResponse> addMovie(
            @Valid @RequestBody MovieRequest movieRequest
    ){
        return ResponseEntity.status(HttpStatus.CREATED).
                body(contentService.addMovie(movieRequest));

    }

    // Get All movies
    @GetMapping
    public ResponseEntity<List<MovieResponse>> getAllMovies(){
        return ResponseEntity.ok().body(contentService.getAllMovies());
    }

    // Get movies by genre
    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<MovieResponse>> getMoviesByGenre(
            @PathVariable Genre genre
    ){
        return ResponseEntity.ok(contentService.getMoviesByGenre(genre));
    }

    // get movie by id
    @GetMapping("/{movieId}")
    public ResponseEntity<MovieResponse> getMoviesById(
            @PathVariable String movieId
    ){
        return ResponseEntity.ok(contentService.getMoviesById(movieId));
    }


    // search movies
    @GetMapping("/search")
    public ResponseEntity<List<MovieResponse>> searchMovies(
            @RequestParam String title
    ){
        return ResponseEntity.ok(contentService.searchMovies(title));
    }

}
