package ru.practicum.ewm.dto.event;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PublicEventRequestParam {
    String text;
    List<Long> categories;
    Boolean paid;
    String rangeStart;
    String rangeEnd;
    Boolean onlyAvailable;
    String sort;
    Integer from;
    Integer size;
}