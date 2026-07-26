package ru.practicum.ewm.error.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.contract.request.RequestMicroserviceOperations;
import ru.practicum.ewm.dto.event.ConfirmedRequestCount;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.dto.request.RequestStatus;
import ru.practicum.ewm.exception.microservice.RequestMicroserviceUnavailableException;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class RequestMicroserviceFallbackFactory implements FallbackFactory<RequestMicroserviceOperations> {

    @Override
    public RequestMicroserviceOperations create(Throwable cause) {
        return new RequestMicroserviceOperations() {

            @Override
            public List<ConfirmedRequestCount> getConfirmedRequestsCount(List<Long> eventIds,
                                                                         RequestStatus requestStatus) {
                log.error("Ошибка вызова метода --getConfirmedRequestsCount-- сервиса заявок (request-service). " +
                        "Причина ошибки: {}.", cause.getMessage());
                log.error("В ответе будет возвращен пустой список подтвержденных заявок.");
                return new ArrayList<>();
            }

            @Override
            public List<ParticipationRequestDto> getRequestsByEventId(Long eventId) {
                log.error("Ошибка вызова метода --getRequestsByEventId-- сервиса заявок (request-service). " +
                        "Причина ошибки: {}.", cause.getMessage());
                log.error("В ответе будет возвращен пустой список запросов на участие в событии.");
                return new ArrayList<>();
            }

            @Override
            public Long getRequestsCountByIdAndStatus(Long eventId, RequestStatus requestStatus) {
                log.error("Ошибка вызова метода --getRequestsCountByIdAndStatus-- сервиса заявок (request-service) " +
                        "для получения данных о событии с ID: {} и статусом: {}. Причина ошибки: {}.",
                        eventId, requestStatus.name(), cause.getMessage(), cause);
                throw new RequestMicroserviceUnavailableException("Сервис заявок временно недоступен. " +
                        "Не удалось получить количество заявок со статусом: " + requestStatus.name() +
                        " для события с ID: " + eventId);
            }

            @Override
            public List<ParticipationRequestDto> getRequests(List<Long> requestIds) {
                log.error("Ошибка вызова метода --getRequests-- сервиса заявок (request-service) " +
                                "для получения списка заявок на участие по событиям с ID: {}. Причина ошибки: {}.",
                        requestIds, cause.getMessage(), cause);
                throw new RequestMicroserviceUnavailableException("Сервис заявок временно недоступен. " +
                        "Не удалось получить список заявок на участие по событиям.");
            }

            @Override
            public List<ParticipationRequestDto> updateRequests(List<ParticipationRequestDto> updatedRequests) {
                log.error("Ошибка вызова метода --updateRequests-- сервиса заявок (request-service) " +
                                "для обновления данных о заявках на участие в событиях: {}. Причина ошибки: {}.",
                        updatedRequests, cause.getMessage(), cause);
                throw new RequestMicroserviceUnavailableException("Сервис заявок временно недоступен. " +
                        "Не удалось обновить данные о заявках на участие в событиях.");
            }

        };
    }

}