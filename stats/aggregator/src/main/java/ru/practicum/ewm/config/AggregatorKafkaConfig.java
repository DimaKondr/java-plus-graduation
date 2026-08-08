package ru.practicum.ewm.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties("spring.kafka")
public class AggregatorKafkaConfig {
    private String bootstrapServers;
    private Consumer consumer = new Consumer();
    private Producer producer = new Producer();

    @Getter
    @Setter
    public static class Consumer {
        private String keyDeserializer;
        private String valueDeserializer;
        private String autoOffsetReset;
        private String groupId;
    }

    @Getter
    @Setter
    public static class Producer {
        private String keySerializer;
        private String valueSerializer;
    }

    @Bean
    public ProducerFactory<String, SpecificRecordBase> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, producer.getKeySerializer());
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, producer.getValueSerializer());
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, SpecificRecordBase> kafkaTemplate(
            ProducerFactory<String, SpecificRecordBase> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

}