package ru.practicum.ewm.dto.category;

import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDto {
    Long id;
    @Size(max = 50, message = "Длина имени категории должна быть не более 50 символов")
    String name;
}