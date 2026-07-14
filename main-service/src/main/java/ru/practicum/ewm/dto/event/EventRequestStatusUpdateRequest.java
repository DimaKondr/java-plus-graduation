package ru.practicum.ewm.dto.event;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.practicum.ewm.model.request.RequestStatus;

import java.util.List;

@Value
@Builder
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class EventRequestStatusUpdateRequest {

    @NotNull(message = "Список с ID запросов не должен быть null")
    List<Long> requestIds;

    @NotNull(message = "Статус запроса не должен быть null")
    RequestStatus status;
}