# FlixGrid

A Netflix-style video streaming platform built with Spring Boot microservices, Apache Kafka, AWS S3 and Redis. Videos are uploaded, encoded into adaptive-bitrate HLS (1080p / 720p / 480p / 360p) with FFmpeg, stored in S3 and streamed to the browser through short-lived presigned URLs.

## Architecture

```
Upload --> video-service --> S3 (raw video)
               |
               +-- VideoUploadedEvent --> Kafka
                                            |
                                    encoding-service
                     (download -> FFmpeg HLS -> master.m3u8 -> upload to S3)
                                            |
                                  VideoEncodedEvent --> Kafka
                                            |
               content-service (metadata, MySQL)    streaming-service (Redis, presigned URLs)
                                                                   |
                                                       Browser (hls.js player)
```

| Service | Port | Responsibility |
|---|---|---|
| content-service | 8081 | Movie metadata and status (MySQL) |
| video-service | 8082 | Receives uploads, stores raw video in S3, publishes events |
| encoding-service | 8083 | FFmpeg HLS encoding, uploads results to S3 |
| streaming-service | 8084 | Serves presigned HLS URLs and signed playlists (Redis cache) |

## Prerequisites

- Java 17+ and Maven
- MySQL (database `content_db`)
- Apache Kafka (`localhost:9092`)
- Redis (`localhost:6379`)
- FFmpeg installed locally
- An AWS account with an S3 bucket and an IAM user that can read and write that bucket

## Configuration

No secrets are stored in this repository. Each service reads its settings from environment variables, so set them in your IDE (in IntelliJ: Run, Edit Configurations, Environment variables) or in your shell before starting a service.

| Service | Variables |
|---|---|
| video-service | AWS_ACCESS_KEY, AWS_SECRET_KEY, AWS_BUCKET_NAME, AWS_REGION |
| encoding-service | AWS_ACCESS_KEY, AWS_SECRET_KEY, AWS_BUCKET_NAME, AWS_REGION, FFMPEG_PATH, TEMP_DIR |
| streaming-service | AWS_ACCESS_KEY, AWS_SECRET_KEY, AWS_BUCKET_NAME, AWS_REGION |
| content-service | DB_PASSWORD |

## Running locally

1. Start MySQL, Kafka and Redis.
2. Set the environment variables for each service.
3. Start the services: content-service, streaming-service, encoding-service, video-service.
4. Upload a video through video-service and wait for the encoding-service log to show that all files were uploaded.
5. Serve the test player over HTTP (not file://):

```
   cd player
   python -m http.server 5500
```

   Open http://localhost:5500/flixgrid-player.html, enter the movie UUID and press Play.

## S3 bucket CORS

For the browser player to load playlists and segments, add this under Bucket, Permissions, CORS:

```json
[
  {
    "AllowedOrigins": ["http://localhost:5500"],
    "AllowedMethods": ["GET", "HEAD"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["Content-Length", "Content-Range"]
  }
]
```

## Encoding notes

Every rendition is encoded as H.264 High profile, level 4.0, yuv420p, with AAC audio, and the master playlist declares CODECS="avc1.640028,mp4a.40.2". The codec string must match the real encoding, otherwise browsers refuse to play the stream.

## Security

- Credentials are supplied only through environment variables.
- Presigned URLs expire after a short time.
- Use an IAM user with the minimum S3 permissions needed, and rotate keys if they are ever exposed.

## Tech stack

Spring Boot, Apache Kafka, AWS S3, Redis, MySQL, FFmpeg (HLS), hls.js
