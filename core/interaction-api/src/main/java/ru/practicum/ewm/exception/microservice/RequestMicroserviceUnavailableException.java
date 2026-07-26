package ru.practicum.ewm.exception.microservice;

public class RequestMicroserviceUnavailableException extends RuntimeException {

    public RequestMicroserviceUnavailableException(String message) {
        super(message);
    }

}