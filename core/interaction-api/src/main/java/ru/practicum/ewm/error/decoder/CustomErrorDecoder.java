package ru.practicum.ewm.error.decoder;

import feign.Response;
import feign.codec.ErrorDecoder;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.MicroserviceInternalServerErrorException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;

public class CustomErrorDecoder implements ErrorDecoder {
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        String message = "При вызове метода " + methodKey + " произошла ошибка: код ошибки >>> " + response.status()
                + ", причина ошибки >>> " + response.reason() + ", тело ответа ошибки: >>> " + response.body() + ".";

        return switch (response.status()) {
            case 400 -> new ValidationException(message);
            case 404 -> new NotFoundException(message);
            case 409 -> new ConflictException(message);
            case 500 -> new MicroserviceInternalServerErrorException(message);
            default -> defaultDecoder.decode(methodKey, response);
        };
    }

}