package ru.practicum.ewm.exception.microservice;

public class EventMicroserviceUnavailableException extends RuntimeException {

    public EventMicroserviceUnavailableException(String message) {
        super(message);
    }

}