package ru.practicum.ewm.controller;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.CollectorGrpcClient;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.PublicEventRequestParam;
import ru.practicum.ewm.service.EventService;
import ru.practicum.ewm.stats.proto.ActionTypeProto;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class EventPublicController {
    private final EventService eventService;
    private final CollectorGrpcClient collectorGrpcClient;

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
                @Positive Integer size
    ) {
        log.info("Уровень Public. Получение списка из {} событий по необходимым параметрам. " +
                "Пропускаем {} элементов. ", size, from);

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
        log.info("Успешный публичный запрос на получение списка событий по фильтрам: {}.", result);

        return result;
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEventByIdByPublicRequest(
            @PathVariable @Positive Long eventId,
            @RequestHeader("X-EWM-USER-ID") Long userId
    ) {
        log.info("Уровень Public. Получение данных о событии с ID: {} пользователем с ID: {}.", eventId, userId);

        EventFullDto result = eventService.getEventByIdByPublicRequest(eventId);

        collectorGrpcClient.collectUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
        log.info("Успешный публичный запрос на получение события по ID. " +
                "В Collector отправлена новая запись о просмотре пользователем с ID: {} события с ID: {}.",
                userId, eventId);

        return result;
    }

    @GetMapping("/recommendations")
    public List<EventFullDto> getRecommendationForUser(@RequestHeader("X-EWM-USER-ID") Long userId) {
        log.info("Уровень Public. Получение списка рекомендованных событий для пользователя с ID: {}.", userId);
        List<EventFullDto> result = eventService.getRecommendationForUser(userId);
        log.info("Успешный публичный запрос на получение списка рекомендованных событий: {}.", result);
        return result;
    }

    @PutMapping("/{eventId}/like")
    public void sendLikeOfEvent(@PathVariable @Positive Long eventId,
                                @RequestHeader("X-EWM-USER-ID") Long userId) {
        log.info("Уровень Public. Пользователь с ID: {} поставил лайк событию с ID: {}.", userId, eventId);
        eventService.sendLikeOfEvent(userId, eventId, ActionTypeProto.ACTION_LIKE);
        log.info("Успешный публичный запрос на применения лайка событию. " +
                "В Collector отправлена новая запись о постановке лайка событию с ID: {} пользователем с ID: {}.",
                eventId, userId);
    }

}