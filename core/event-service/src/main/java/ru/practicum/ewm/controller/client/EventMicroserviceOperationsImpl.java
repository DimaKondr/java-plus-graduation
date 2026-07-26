package ru.practicum.ewm.controller.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.contract.event.EventMicroserviceOperations;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.service.EventService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/microservice/event-service")
@Validated
public class EventMicroserviceOperationsImpl implements EventMicroserviceOperations {
    private final EventService eventService;

    @Override
    public EventFullDto getEventByIdForMicroservice(Long eventId) {
        log.info("GET /microservice/event-service/events/event/{}", eventId);
        return eventService.getEventByIdForMicroservice(eventId);
    }

    @Override
    public Boolean isEventExist(Long eventId) {
        log.info("GET /microservice/event-service/events/is-exist/{}", eventId);
        return eventService.isEventExist(eventId);
    }

}