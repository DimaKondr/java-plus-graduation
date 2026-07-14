package ru.practicum.ewm.dto.event;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Value
@Builder
@Getter
@AllArgsConstructor
public class LocationDto {

    @NotNull(message = "Координаты локации события. Широта не должна быть null")
    @DecimalMin(value = "-90.0", message = "Широта должна быть от -90 до 90")
    @DecimalMax(value = "90.0", message = "Широта должна быть от -90 до 90")
    Double lat;

    @NotNull(message = "Координаты локации события. Долгота не должна быть null")
    @DecimalMin(value = "-180.0", message = "Долгота должна быть от -180 до 180")
    @DecimalMax(value = "180.0", message = "Долгота должна быть от -180 до 180")
    Double lon;
}