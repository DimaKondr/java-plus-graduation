package ru.practicum.ewm.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.StatClient;
import ru.practicum.ewm.HitDto;
import ru.practicum.ewm.constants.Constants;
import ru.practicum.ewm.dto.event.*;
import ru.practicum.ewm.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class EventPublicController {
    private final EventService eventService;
    private final StatClient statClient;

    @GetMapping
    public List<EventShortDto> getEventsByPublicRequest(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(defaultValue = "false")
                Boolean onlyAvailable,
            @RequestParam(required = false)
                @Pattern(regexp = "EVENT_DATE|VIEWS", message = "Сортировка возможная только по EVENT_DATE или VIEWS.")
                String sort,
            @RequestParam(defaultValue = "0")
                @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10")
                @Positive Integer size,
            HttpServletRequest request
    ) {
        log.info("Уровень Public. Получение списка из {} событий по необходимым параметрам. " +
                "Пропускаем {} элементов. ", size, from);
        HitDto hitDto = HitDto.builder()
                .app("ewm-main-service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now().format(Constants.FORMATTER))
                .build();
        HitDto hitResult = statClient.postHit(hitDto);
        PublicEventRequestParam param = PublicEventRequestParam.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(onlyAvailable)
                .sort(sort)
                .from(from)
                .size(size)
                .build();
        List<EventShortDto> result = eventService.getEventsByPublicRequest(param);
        log.info("Успешный публичный запрос на получение списка событий по фильтрам. " +
                "В статистику внесена новая запись: {}", hitResult);
        return result;
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEventByIdByPublicRequest(
            @PathVariable @Positive Long eventId,
            HttpServletRequest request
    ) {
        log.info("Уровень Public. Получение данных о событии с ID: {}. ",eventId);
        HitDto hitDto = HitDto.builder()
                .app("ewm-main-service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now().format(Constants.FORMATTER))
                .build();
        HitDto hitResult = statClient.postHit(hitDto);
        EventFullDto result = eventService.getEventByIdByPublicRequest(eventId);
        log.info("Успешный публичный запрос на получение события по ID. " +
                "В статистику внесена новая запись: {}", hitResult);
        return result;
    }

}