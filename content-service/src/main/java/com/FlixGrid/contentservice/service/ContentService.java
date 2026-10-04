package com.FlixGrid.contentservice.service;

import com.FlixGrid.contentservice.dto.MovieRequest;
import com.FlixGrid.contentservice.dto.MovieResponse;
import com.FlixGrid.contentservice.model.Genre;
import com.FlixGrid.contentservice.model.Movie;
import com.FlixGrid.contentservice.model.VideoStatus;
import com.FlixGrid.contentservice.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContentService {

    private final MovieRepository movieRepository;


    // Add a new movie to catalog
    // video is not uploaded yet at this stage to S3

    public MovieResponse addMovie(MovieRequest request){
        log.info("Adding new movie : {}", request.getTitle());
        Movie movie = new Movie();
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setGenre(Genre.valueOf(request.getGenre()));
        movie.setDirector(request.getDirector());
        movie.setCast(request.getCast());
        movie.setReleaseYear(Integer.parseInt(request.getReleaseYear()));
        movie.setRating(request.getRating());
        movie.setThumbnailUrl(request.getThumbnailUrl());
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setVideoStatus(VideoStatus.PENDING);

        Movie savedMovie = movieRepository.save(movie);
        log.info("Movie Added with ID : {}", savedMovie.getId());

        return mapToResponse(savedMovie);
    }



    // get all movies
    public List<MovieResponse> getAllMovies(){
        return movieRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // get movie by ID
    public MovieResponse getMoviesById(String movieId){

        Movie movie = movieRepository.findById(movieId).
                orElseThrow(()-> new RuntimeException("Movie not found with ID :"+movieId));
        //if found
        return mapToResponse(movie);
    }

    // get movie by genre
    public List<MovieResponse> getMoviesByGenre(Genre genre){
        return movieRepository.findByGenre(genre)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // search movie by title
    public List<MovieResponse> searchMovies(String title){
        return movieRepository.findByTitleContainingIgnoreCase(title)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // the content service add the movie to db
    // and the video service then upload that movie to s3
    // when a movie is uploaded to s3 then we decide a key for that movie to identify it
    public void updateVideoKey(String movieId, String videoKey){
        log.info("Updating videoKey for movie :{}", movieId);
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(()-> new RuntimeException("Movie not found with ID :"+movieId));
        movie.setVideoKey(videoKey);
        movie.setVideoStatus(VideoStatus.UPLOADED);
        movieRepository.save(movie);
    }

    // update the hlsUrl
    public void updateHlsUrl(String movieId, String hlsUrl){
        log.info("Updating hlsUrl for movie :{}", movieId);
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(()-> new RuntimeException("Movie not found with ID :"+movieId));
        movie.setHlsUrl(hlsUrl);
        movie.setVideoStatus(VideoStatus.READY);
        movieRepository.save(movie);
        log.info("Movie {} is ready for streaming" , movieId);
    }

    public void updateVideoStatus(String movieId, VideoStatus videoStatus){
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(()-> new RuntimeException("Movie not found with ID :"+movieId));
        movie.setVideoStatus(videoStatus);
        movieRepository.save(movie);
    }


    private MovieResponse mapToResponse(Movie movie){
        MovieResponse response = new MovieResponse();
        response.setId(movie.getId());
        response.setTitle(movie.getTitle());
        response.setDescription(movie.getDescription());
        response.setGenre(movie.getGenre().toString());
        response.setDirector(movie.getDirector().toString());
        response.setCast(movie.getCast().toString());
        response.setReleaseYear(String.valueOf(movie.getReleaseYear()));
        response.setRating(response.getRating());
        response.setThumbnailUrl(movie.getThumbnailUrl());
        response.setDurationMinutes(movie.getDurationMinutes());
        response.setVideoKey(movie.getVideoKey());
        response.setVideoStatus(movie.getVideoStatus());
        response.setHlsUrl(movie.getHlsUrl());
        response.setCreatedAt(movie.getCreatedAt());

        return  response;


    }
}
