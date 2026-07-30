package ru.practicum.ewm.exception.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.ewm.exception.microservice.EventMicroserviceUnavailableException;
import ru.practicum.ewm.exception.microservice.RequestMicroserviceUnavailableException;
import ru.practicum.ewm.exception.microservice.UserMicroserviceUnavailableException;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MicroserviceIntegrationErrorHandler {

    @ExceptionHandler(EventMicroserviceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError handleEventMicroserviceUnavailable(EventMicroserviceUnavailableException ex) {
        log.error("Ошибка вызова сервиса событий (event-service): {}.", ex.getMessage());

        return ApiError.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE.name())
                .reason("Сервис событий временно недоступен.")
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler(RequestMicroserviceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError handleRequestMicroserviceUnavailable(RequestMicroserviceUnavailableException ex) {
        log.error("Ошибка вызова сервиса заявок (request-service): {}.", ex.getMessage());

        return ApiError.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE.name())
                .reason("Сервис заявок временно недоступен.")
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler(UserMicroserviceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError handleUserMicroserviceUnavailable(UserMicroserviceUnavailableException ex) {
        log.error("Ошибка вызова сервиса пользователей (user-service): {}.", ex.getMessage());

        return ApiError.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE.name())
                .reason("Сервис пользователей временно недоступен.")
                .message(ex.getMessage())
                .build();
    }

    @ExceptionHandler({feign.RetryableException.class, feign.FeignException.class})
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError handleFeignDirectExceptions(Exception ex) {
        log.error("Сбой подключения после всех повторных попыток: {}.", ex.getMessage());

        String serviceName = "Внешний микросервис";
        if (ex.getMessage().contains("request-service")) serviceName = "Сервис заявок (request-service)";
        if (ex.getMessage().contains("event-service")) serviceName = "Сервис событий (event-service)";
        if (ex.getMessage().contains("user-service")) serviceName = "Сервис пользователей (user-service)";

        return ApiError.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE.name())
                .reason(serviceName + " временно недоступен.")
                .message("Не удалось получить ответ от внешнего микросервиса после серии повторных попыток.")
                .build();
    }

}