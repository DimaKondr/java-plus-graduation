package ru.practicum.ewm.contract.event;

import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.error.fallback.EventMicroserviceFallbackFactory;

@FeignClient(
        name = "event-service",
        path = "/microservice/event-service",
        fallback = EventMicroserviceFallbackFactory.class
)
public interface EventMicroserviceOperations {

    @GetMapping("events/event/{eventId}")
    EventFullDto getEventByIdForMicroservice(@PathVariable @Positive Long eventId);

    @GetMapping("events/is-exist/{eventId}")
    Boolean isEventExist(@PathVariable @Positive Long eventId);

}