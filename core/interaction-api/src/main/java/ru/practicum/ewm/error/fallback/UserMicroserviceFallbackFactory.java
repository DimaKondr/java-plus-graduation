package ru.practicum.ewm.error.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.contract.user.UserMicroserviceOperations;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.exception.microservice.UserMicroserviceUnavailableException;

import java.util.List;

@Component
@Slf4j
public class UserMicroserviceFallbackFactory implements FallbackFactory<UserMicroserviceOperations> {

    @Override
    public UserMicroserviceOperations create(Throwable cause) {
        return new UserMicroserviceOperations() {

            @Override
            public Boolean isUserExist(Long userId) {
                log.error("Ошибка вызова метода --isUserExist-- сервиса пользователей (user-service) " +
                                "для получения подтверждения о наличии пользователя с ID: {}. Причина ошибки: {}.",
                        userId, cause.getMessage(),cause);
                throw new UserMicroserviceUnavailableException("Сервис пользователей временно недоступен. " +
                        "Не удалось получить подтверждение наличия пользователя с ID: " + userId);
            }

            @Override
            public UserShortDto getUserShortInfo(Long userId) {
                log.error("Ошибка вызова метода --getUserShortInfo-- сервиса пользователей (user-service) " +
                                "для получения данных о пользователе с ID: {}. Причина ошибки: {}.",
                        userId, cause.getMessage(),cause);
                throw new UserMicroserviceUnavailableException("Сервис пользователей временно недоступен. " +
                        "Не удалось получить данные о пользователе с ID: " + userId);
            }

            @Override
            public List<UserShortDto> getShortInfoOfUsers(List<Long> usersIds) {
                log.error("Ошибка вызова метода --getShortInfoOfUsers-- сервиса пользователей (user-service) " +
                                "для получения списка данных о пользователях с ID: {}. Причина ошибки: {}.",
                        usersIds, cause.getMessage(),cause);
                throw new UserMicroserviceUnavailableException("Сервис пользователей временно недоступен. " +
                        "Не удалось получить список данных о пользователях с ID: " + usersIds);
            }

        };
    }

}