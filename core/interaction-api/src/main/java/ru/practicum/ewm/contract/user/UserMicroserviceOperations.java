package ru.practicum.ewm.contract.user;

import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.error.fallback.UserMicroserviceFallbackFactory;

import java.util.List;

@FeignClient(
        name = "user-service",
        path = "/microservice/user-service",
        fallback = UserMicroserviceFallbackFactory.class
)
public interface UserMicroserviceOperations {

    @GetMapping("users/{userId}")
    Boolean isUserExist(@PathVariable @Positive Long userId);

    @GetMapping("users/{userId}/user")
    UserShortDto getUserShortInfo(@PathVariable @Positive Long userId);

    @PostMapping("users/ids")
    List<UserShortDto> getShortInfoOfUsers(@RequestBody List<Long> usersIds);

}