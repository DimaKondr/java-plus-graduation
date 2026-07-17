package ru.practicum.ewm.error;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public class ApiError {
    private final HttpStatus status;
    private final String description;
    private final String error;
    private final String stackTrace;
}