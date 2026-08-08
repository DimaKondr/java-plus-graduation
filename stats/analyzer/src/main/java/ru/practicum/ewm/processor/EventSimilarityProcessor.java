package ru.practicum.ewm.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.broker.AnalyzerTopics;
import ru.practicum.ewm.config.EventSimilarityConsumerConfig;
import ru.practicum.ewm.mapper.EventSimilarityMapper;
import ru.practicum.ewm.model.EventSimilarity;
import ru.practicum.ewm.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Component
@Slf4j
@RequiredArgsConstructor
public class EventSimilarityProcessor implements Runnable {
    private final EventSimilarityRepository eventSimilarityRepository;
    private final EventSimilarityConsumerConfig consumerConfig;
    private final EventSimilarityMapper eventSimilarityMapper;
    private KafkaConsumer<String, EventSimilarityAvro> consumer;

    public void run() {
        try {
            Properties properties = new Properties();
            properties.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    consumerConfig.getBootstrapServers());
            properties.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                    consumerConfig.getEventSimilarityConsumer().getKeyDeserializer());
            properties.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                    consumerConfig.getEventSimilarityConsumer().getValueDeserializer());
            properties.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                    consumerConfig.getEventSimilarityConsumer().getAutoOffsetReset());
            properties.put(ConsumerConfig.GROUP_ID_CONFIG,
                    consumerConfig.getEventSimilarityConsumer().getGroupId());

            this.consumer = new KafkaConsumer<>(properties);

            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            consumer.subscribe(List.of(AnalyzerTopics.STATS_EVENTS_SIMILARITY_V1));

            while (true) {
                ConsumerRecords<String, EventSimilarityAvro> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, EventSimilarityAvro> record : records) {
                    log.info("Поступили данные о коэффициенте сходства: {}", record.value());
                    handleEventSimilarity(record.value());
                }
            }
        } catch (WakeupException ignored) {
            log.info("Получен сигнал остановки (WakeupException)");
        } catch (Exception e) {
            log.error("Ошибка во время получения данных", e);
        } finally {
            try {
                log.info("Фиксация смещений перед закрытием...");
                if (consumer != null) {
                    consumer.commitSync();
                }
            } catch (Exception e) {
                log.error("Ошибка при финальном сбросе данных или коммите оффсетов", e);
            } finally {
                if (consumer != null) {
                    log.info("Закрываем консьюмер");
                    consumer.close();
                }
            }
        }
    }

    private void handleEventSimilarity(EventSimilarityAvro avro) {
        EventSimilarity eventSimilarity = eventSimilarityMapper.toEntity(avro);

        Optional<EventSimilarity> existing = eventSimilarityRepository
                .findByEvent1AndEvent2(eventSimilarity.getEvent1(), eventSimilarity.getEvent2());

        if (existing.isPresent()) {
            EventSimilarity existingSimilarity = existing.get();
            existingSimilarity.setSimilarity(eventSimilarity.getSimilarity());
            existingSimilarity.setTs(eventSimilarity.getTs());
            eventSimilarityRepository.save(existingSimilarity);
            log.info("Обновление данных о коэффициенте сходства событий {} и {}. Коэффициенте сходства = {}.",
                    eventSimilarity.getEvent1(),
                    eventSimilarity.getEvent2(),
                    eventSimilarity.getSimilarity());
        } else {
            eventSimilarityRepository.save(eventSimilarity);
            log.info("Добавлены новые данные о коэффициенте сходства событий {} и {}, Коэффициенте сходства = {}.",
                    eventSimilarity.getEvent1(),
                    eventSimilarity.getEvent2(),
                    eventSimilarity.getSimilarity());
        }
    }

}