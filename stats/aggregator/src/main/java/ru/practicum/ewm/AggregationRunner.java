package ru.practicum.ewm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.broker.AggregatorTopics;
import ru.practicum.ewm.config.AggregatorKafkaConfig;
import ru.practicum.ewm.handler.UserActionHandler;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Component
@RequiredArgsConstructor
@Slf4j
public class AggregationRunner implements CommandLineRunner {
    private final AggregatorKafkaConfig consumerConfig;
    private final UserActionHandler handler;
    private final KafkaTemplate<String, SpecificRecordBase> producer;
    private KafkaConsumer<String, UserActionAvro> consumer;

    @Override
    public void run(String... args) throws Exception {
        try {
            Properties properties = new Properties();
            properties.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    consumerConfig.getBootstrapServers());
            properties.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                    consumerConfig.getConsumer().getKeyDeserializer());
            properties.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                    consumerConfig.getConsumer().getValueDeserializer());
            properties.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                    consumerConfig.getConsumer().getAutoOffsetReset());
            properties.put(ConsumerConfig.GROUP_ID_CONFIG,
                    consumerConfig.getConsumer().getGroupId());
            this.consumer = new KafkaConsumer<>(properties);

            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            consumer.subscribe(List.of(AggregatorTopics.STATS_USER_ACTIONS_V1));

            while (true) {
                ConsumerRecords<String, UserActionAvro> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    log.info("Поступили данные о действии пользователя: {}.", record.value());

                    List<EventSimilarityAvro> eventsSimilarity = handler.handleUserAction(record.value());

                    for (EventSimilarityAvro eventSimilarity : eventsSimilarity) {
                        log.info("Готовы данные о сходстве мероприятий в формате Avro:" +
                                        " >>> {} <<< для отправки в Kafka-топик: >>> {} <<<.",
                                eventSimilarity, AggregatorTopics.STATS_EVENTS_SIMILARITY_V1);

                        producer.send(AggregatorTopics.STATS_EVENTS_SIMILARITY_V1, eventSimilarity);
                    }
                }
            }

        } catch (WakeupException ignored) {
            log.info("Cервис Aggregator. Получен сигнал остановки (WakeupException).");
        } catch (Exception e) {
            log.error("Cервис Aggregator. Ошибка во время обработки данных о действиях пользователей.", e);
        } finally {
            try {
                log.info("Cервис Aggregator. Сброс буферов и фиксация смещений перед закрытием...");

                if (producer != null) {
                    producer.flush();
                }

                if (consumer != null) {
                    consumer.commitSync();
                }

            } catch (Exception e) {
                log.error("Cервис Aggregator. Ошибка при финальном сбросе данных или коммите оффсетов.", e);
            } finally {
                if (consumer != null) {
                    log.info("Cервис Aggregator. Закрываем консьюмер.");
                    consumer.close();
                }

                if (producer != null) {
                    log.info("Cервис Aggregator. Закрываем продюсер.");
                    producer.destroy();
                }
            }
        }
    }

}