package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.contract.user.UserMicroserviceOperations;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/microservice/user-service")
@Validated
public class UserMicroserviceOperationsImpl implements UserMicroserviceOperations {
    private final UserService userService;

    @Override
    public Boolean isUserExist(Long userId) {
        log.info("GET /microservice/user-service/users/{}", userId);
        return userService.isUserExist(userId);
    }

    @Override
    public UserShortDto getUserShortInfo(Long userId) {
        log.info("GET /microservice/user-service/users/{}/user", userId);
        return userService.getUserShortInfo(userId);
    }

    @Override
    public List<UserShortDto> getShortInfoOfUsers(List<Long> usersIds) {
        log.info("GET /microservice/user-service/users/ids");
        return userService.getShortInfoOfUsers(usersIds);
    }

}