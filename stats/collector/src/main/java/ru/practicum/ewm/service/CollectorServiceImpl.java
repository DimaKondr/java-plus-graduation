package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.broker.CollectorTopics;
import ru.practicum.ewm.mapper.UserActionMapper;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.UserActionProto;

@Service
@Slf4j
@RequiredArgsConstructor
public class CollectorServiceImpl implements CollectorService {
    private final KafkaTemplate<Long, SpecificRecordBase> producer;
    private final UserActionMapper mapper;

    @Override
    public void collectUserAction(UserActionProto userAction) {
        try {
            UserActionAvro userActionAvro = mapper.toAvro(userAction);
            String topic = CollectorTopics.STATS_USER_ACTIONS_V1;
            log.info("Готовы данные о действии пользователя в формате Avro:" +
                            " >>> {} <<< для отправки в Kafka-топик: >>> {} <<<.",
                    userActionAvro, CollectorTopics.STATS_USER_ACTIONS_V1);
            producer.send(topic, userAction.getUserId(), userActionAvro);
        } catch (Exception e) {
            log.error("Cервис Collector. Ошибка во время обработки данных о действии пользователя.", e);
        } finally {
            try {
                log.info("Cервис Collector. Сброс буферов перед закрытием...");
                if (producer != null) {
                    producer.flush();
                }
            } catch (Exception e) {
                log.error("Cервис Collector. Ошибка при финальном сбросе данных.", e);
            } finally {
                if (producer != null) {
                    log.info("Collector. Закрываем продюсер.");
                    producer.destroy();
                }
            }
        }
    }

}