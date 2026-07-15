package ru.practicum.ewm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatRequestParamDto {

    @NotBlank(message = "start не может быть пустым")
    @Pattern(
            regexp = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$",
            message = "Неверный формат start. Используйте yyyy-MM-dd HH:mm:ss"
    )
    private String start;

    @NotBlank(message = "end не может быть пустым")
    @Pattern(
            regexp = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$",
            message = "Неверный формат end. Используйте yyyy-MM-dd HH:mm:ss"
    )
    private String end;

    private List<String> uris;

    @NotNull(message = "unique не может быть null")
    private Boolean unique;
}