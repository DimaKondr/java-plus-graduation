package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.contract.request.RequestMicroserviceOperations;
import ru.practicum.ewm.dto.event.ConfirmedRequestCount;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.dto.request.RequestStatus;
import ru.practicum.ewm.service.RequestService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/microservice/request-service")
@Validated
public class RequestMicroserviceOperationsImpl implements RequestMicroserviceOperations {
    private final RequestService requestService;

    @Override
    public List<ConfirmedRequestCount> getConfirmedRequestsCount(List<Long> eventIds, RequestStatus requestStatus) {
        log.info("GET /microservice/request-service/request/confirmed-requests-count");
        return requestService.countConfirmedRequests(eventIds, requestStatus);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByEventId(Long eventId) {
        log.info("GET /microservice/request-service/request/event/{}", eventId);
        return requestService.getRequestsByEventId(eventId);
    }

    @Override
    public Long getRequestsCountByIdAndStatus(Long eventId, RequestStatus requestStatus) {
        log.info("GET /microservice/request-service/request/count");
        return requestService.getRequestsCountByIdAndStatus(eventId, requestStatus);
    }

    @Override
    public List<ParticipationRequestDto> getRequests(List<Long> requestIds) {
        log.info("GET /microservice/request-service/request/event");
        return requestService.getRequests(requestIds);
    }

    @Override
    public List<ParticipationRequestDto> updateRequests(List<ParticipationRequestDto> updatedRequests) {
        log.info("PATCH /microservice/request-service/request/event");
        return requestService.updateRequests(updatedRequests);
    }

}