package ru.practicum.ewm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("custom.kafka-event-similarity")
public class EventSimilarityConsumerConfig {

    @Value("${custom.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private EventSimilarityConsumer eventSimilarityConsumer = new EventSimilarityConsumer();

    @Getter
    @Setter
    public static class EventSimilarityConsumer {
        private String keyDeserializer;
        private String valueDeserializer;
        private String autoOffsetReset;
        private String groupId;
    }

}