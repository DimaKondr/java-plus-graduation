package ru.practicum.ewm.service.integration;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.contract.user.UserMicroserviceOperations;
import ru.practicum.ewm.dto.user.UserShortDto;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserIntegrationService {
    private final UserMicroserviceOperations userMicroserviceClient;

    @Retry(name = "userServiceRetry")
    public UserShortDto getUserInfo(Long userId) {
        return userMicroserviceClient.getUserShortInfo(userId);
    }

    @Retry(name = "userServiceRetry")
    public List<UserShortDto> getInfoOfUsers(List<Long> usersIds) {
        return userMicroserviceClient.getShortInfoOfUsers(usersIds);
    }

    @Retry(name = "userServiceRetry")
    public Boolean checkUserExisting(Long userId) {
        return userMicroserviceClient.isUserExist(userId);
    }

}