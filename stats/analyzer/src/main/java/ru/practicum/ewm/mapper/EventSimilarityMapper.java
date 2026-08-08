package ru.practicum.ewm.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.model.EventSimilarity;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.math.BigDecimal;

@Component
public class EventSimilarityMapper {

    public EventSimilarity toEntity(EventSimilarityAvro avro) {
        return EventSimilarity.builder()
                .event1(avro.getEventA())
                .event2(avro.getEventB())
                .similarity(BigDecimal.valueOf(avro.getScore()))
                .ts(avro.getTimestamp())
                .build();
    }

}