package ru.practicum.ewm.exception;

public class StatsServerUnavailable extends RuntimeException {

    public StatsServerUnavailable(String message) {
        super(message);
    }

    public StatsServerUnavailable(String message, Exception cause) {
        super(message, cause);
    }
}