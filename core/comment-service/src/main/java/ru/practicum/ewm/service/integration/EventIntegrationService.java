package ru.practicum.ewm.service.integration;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.contract.event.EventMicroserviceOperations;
import ru.practicum.ewm.dto.event.EventFullDto;

@Component
@RequiredArgsConstructor
public class EventIntegrationService {
    private final EventMicroserviceOperations eventMicroserviceClient;

    @Retry(name = "eventServiceRetry")
    public EventFullDto getEventInfo(Long eventId) {
        return eventMicroserviceClient.getEventByIdForMicroservice(eventId);
    }

    @Retry(name = "eventServiceRetry")
    public Boolean checkEventExisting(Long eventId) {
        return eventMicroserviceClient.isEventExist(eventId);
    }

}