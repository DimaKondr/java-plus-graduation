package ru.practicum.ewm.error.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.contract.event.EventMicroserviceOperations;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.exception.microservice.EventMicroserviceUnavailableException;

@Component
@Slf4j
public class EventMicroserviceFallbackFactory implements FallbackFactory<EventMicroserviceOperations> {

    @Override
    public EventMicroserviceOperations create(Throwable cause) {
        return new EventMicroserviceOperations() {

            @Override
            public EventFullDto getEventByIdForMicroservice(Long eventId) {
                log.error("Ошибка вызова метода --getEventByIdForMicroservice-- сервиса событий (event-service) " +
                        "для получения данных о событии с ID: {}. Причина ошибки: {}.",
                        eventId, cause.getMessage(), cause);
                throw new EventMicroserviceUnavailableException("Сервис событий временно недоступен. " +
                        "Не удалось получить данные о событии с ID: " + eventId);
            }

            @Override
            public Boolean isEventExist(Long eventId) {
                log.error("Ошибка вызова метода --isEventExist-- сервиса событий (event-service) " +
                        "для получения подтверждения о наличии события с ID: {}. Причина ошибки: {}.",
                        eventId, cause.getMessage(),cause);
                throw new EventMicroserviceUnavailableException("Сервис событий временно недоступен. " +
                        "Не удалось получить подтверждение наличия события с ID: " + eventId);
            }

        };
    }

}