package ru.practicum.ewm.dto.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

@Builder
@Getter
@Setter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class NewEventDto {

    @NotBlank(message = "Аннотация события не может быть null или пустой")
    @Size(min = 20, max = 2000, message = "Длина аннотации должна быть не менее 20 и не более 2000 символов")
    String annotation;

    @NotNull(message = "ID категории не должен быть null")
    Long category;

    @NotBlank(message = "Описание события не может быть null или пустым")
    @Size(min = 20, max = 7000, message = "Длина описания события должна быть не менее 20 и не более 7000 символов")
    String description;

    @NotNull(message = "Дата события не должна быть null")
    String eventDate;

    @NotNull(message = "Координаты локации события не должны быть null")
    @Valid
    LocationDto location;

    Boolean paid;

    @PositiveOrZero(message = "Лимит участников не может быть отрицательным")
    Integer participantLimit;

    Boolean requestModeration;

    @NotBlank(message = "Заголовок события не может быть null или пустым")
    @Size(min = 3, max = 120, message = "Длина заголовка события должна быть не менее 3 и не более 120 символов")
    String title;
}