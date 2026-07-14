package ru.practicum.ewm.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Value
@Getter
@Builder
public class CreateUpdateRequestDto {

    @NotNull
    @PositiveOrZero
    Long userId;

    @NotNull
    @PositiveOrZero
    Long eventId;

}
