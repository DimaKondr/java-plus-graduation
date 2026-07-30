package ru.practicum.ewm.exception;

public class MicroserviceInternalServerErrorException extends RuntimeException {

    public MicroserviceInternalServerErrorException(String message) {
        super(message);
    }

}