package com.FlixGrid.videoservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    // Published when video is uploaded to S3
    // Encoding service consume this
    @Bean
    public NewTopic videoUploadedTopic(){
       return TopicBuilder.name("video.uploaded")
                .partitions(1)
                .replicas(3)
                .build();

    }

    // Published when encoding is complete
    // streaming service consume this
    @Bean
    public NewTopic videoEncodedTopic(){

        return TopicBuilder.name("video.encoded")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
