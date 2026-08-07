package ru.practicum.ewm.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.model.UserAction;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.math.BigDecimal;

@Component
public class UserActionMapper {

    public UserAction toEntity(UserActionAvro avro) {
        return UserAction.builder()
                .userId(avro.getUserId())
                .eventId(avro.getEventId())
                .rating(toRating(avro.getActionType()))
                .ts(avro.getTimestamp())
                .build();
    }

    private BigDecimal toRating(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> BigDecimal.valueOf(0.4);
            case REGISTER -> BigDecimal.valueOf(0.8);
            case LIKE -> BigDecimal.valueOf(1.0);
        };
    }

}