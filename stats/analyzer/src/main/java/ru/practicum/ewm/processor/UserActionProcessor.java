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
import ru.practicum.ewm.config.UserActionConsumerConfig;
import ru.practicum.ewm.mapper.UserActionMapper;
import ru.practicum.ewm.model.UserAction;
import ru.practicum.ewm.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserActionProcessor implements Runnable {
    private final UserActionRepository userActionRepository;
    private final UserActionConsumerConfig consumerConfig;
    private final UserActionMapper userActionMapper;

    private KafkaConsumer<String, UserActionAvro> consumer;

    public void run() {
        try {
            Properties properties = new Properties();
            properties.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    consumerConfig.getBootstrapServers());
            properties.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                    consumerConfig.getUserActionConsumer().getKeyDeserializer());
            properties.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                    consumerConfig.getUserActionConsumer().getValueDeserializer());
            properties.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                    consumerConfig.getUserActionConsumer().getAutoOffsetReset());
            properties.put(ConsumerConfig.GROUP_ID_CONFIG,
                    consumerConfig.getUserActionConsumer().getGroupId());

            this.consumer = new KafkaConsumer<>(properties);

            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            consumer.subscribe(List.of(AnalyzerTopics.STATS_USER_ACTIONS_V1));

            while (true) {
                ConsumerRecords<String, UserActionAvro> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    log.info("Поступили данные о действии пользователя: {}", record.value());
                    handleUserAction(record.value());
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

    private void handleUserAction(UserActionAvro avro) {
        UserAction userAction = userActionMapper.toEntity(avro);

        Optional<UserAction> existing = userActionRepository
                .findByUserIdAndEventId(userAction.getUserId(), userAction.getEventId());

        if (existing.isPresent()) {
            UserAction existingAction = existing.get();

            BigDecimal currentRating = existingAction.getRating();
            BigDecimal newRating = userAction.getRating();

            if (newRating.compareTo(currentRating) > 0) {
                existingAction.setRating(newRating);
                existingAction.setTs(userAction.getTs());
                userActionRepository.save(existingAction);
                log.info("Обновление данных о действии пользователя с ID: {} для события с ID: {}. " +
                                "Новый рейтинг действия пользователя = {} (предыдущий рейтинг = {}).",
                        userAction.getUserId(),
                        userAction.getEventId(),
                        userAction.getRating(),
                        existingAction.getRating());
            } else {
                log.info("Обновление данных о действии пользователя с ID: {} для события с ID: {} не требуется, " +
                                "так как новый рейтинг действия пользователя (новый рейтинг = {}) " +
                                "совпадает с текущим (текущий рейтинг = {}).",
                        userAction.getUserId(),
                        userAction.getEventId(),
                        userAction.getRating(),
                        existingAction.getRating());
            }
        } else {
            userActionRepository.save(userAction);
            log.info("Добавлены новые данные о действии пользователя с ID: {} для события с ID: {}. " +
                            "Новый рейтинг действия пользователя = {}.",
                    userAction.getUserId(),
                    userAction.getEventId(),
                    userAction.getRating());
        }
    }

}