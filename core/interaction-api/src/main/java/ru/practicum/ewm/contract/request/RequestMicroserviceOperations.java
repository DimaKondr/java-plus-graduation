package ru.practicum.ewm.contract.request;

import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.event.ConfirmedRequestCount;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.dto.request.RequestStatus;
import ru.practicum.ewm.error.fallback.RequestMicroserviceFallbackFactory;

import java.util.List;

@FeignClient(
        name = "request-service",
        path = "/microservice/request-service",
        fallback = RequestMicroserviceFallbackFactory.class
)
public interface RequestMicroserviceOperations {

    @PostMapping("request/confirmed-requests-count")
    List<ConfirmedRequestCount> getConfirmedRequestsCount(
            @RequestBody List<Long> eventIds,
            @RequestParam RequestStatus requestStatus
    );

    @GetMapping("request/event/{eventId}")
    List<ParticipationRequestDto> getRequestsByEventId(@PathVariable @Positive Long eventId);

    @GetMapping("request/count")
    Long getRequestsCountByIdAndStatus(
            @RequestParam @Positive Long eventId,
            @RequestParam RequestStatus requestStatus
    );

    @PostMapping("request/ids")
    List<ParticipationRequestDto> getRequests(@RequestBody List<Long> requestIds);

    @PutMapping("request/event")
    List<ParticipationRequestDto> updateRequests(@RequestBody List<ParticipationRequestDto> updatedRequests);

}