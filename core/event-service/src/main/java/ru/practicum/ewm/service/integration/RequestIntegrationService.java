package ru.practicum.ewm.service.integration;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.contract.request.RequestMicroserviceOperations;
import ru.practicum.ewm.dto.event.ConfirmedRequestCount;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.dto.request.RequestStatus;
import ru.practicum.ewm.model.event.Event;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RequestIntegrationService {
    private final RequestMicroserviceOperations requestMicroserviceClient;

    @Retry(name = "requestServiceRetry")
    public List<ParticipationRequestDto> getRequests(Long eventId) {
        return requestMicroserviceClient.getRequestsByEventId(eventId);
    }

    @Retry(name = "requestServiceRetry")
    public List<ParticipationRequestDto> getRequestsByList(List<Long> eventIds) {
        return requestMicroserviceClient.getRequests(eventIds);
    }

    @Retry(name = "requestServiceRetry")
    public Long getRequestsCount(Long eventId, RequestStatus status) {
        return requestMicroserviceClient.getRequestsCountByIdAndStatus(eventId, status);
    }

    @Retry(name = "requestServiceRetry")
    public List<ParticipationRequestDto> updateRequests(List<ParticipationRequestDto> updatedRequests) {
        return requestMicroserviceClient.updateRequests(updatedRequests);
    }

    @Retry(name = "requestServiceRetry")
    public Map<Long, Long> getConfirmedRequestsCount(List<Event> events) {
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        List<ConfirmedRequestCount> count = requestMicroserviceClient
                .getConfirmedRequestsCount(eventIds, RequestStatus.CONFIRMED);

        return count.stream()
                .collect(Collectors.toMap(
                        ConfirmedRequestCount::getEventId,
                        ConfirmedRequestCount::getCount
                ));
    }

}