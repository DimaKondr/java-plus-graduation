package ru.practicum.ewm.dto.event;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.util.List;

@Value
@Builder
@AllArgsConstructor
public class AdminEventRequestParam {
    List<@Positive Long> users;
    List<String> states;
    List<@Positive Long> categories;
    String rangeStart;
    String rangeEnd;

    @PositiveOrZero
    Integer from;

    @Positive
    Integer size;
}