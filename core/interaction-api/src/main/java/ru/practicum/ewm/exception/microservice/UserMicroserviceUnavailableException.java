package ru.practicum.ewm.exception.microservice;

public class UserMicroserviceUnavailableException extends RuntimeException {

    public UserMicroserviceUnavailableException(String message) {
        super(message);
    }

}