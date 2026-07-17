package ru.practicum.ewm.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommentUserRequest {
    private Long id;
    private Long eventId;
    private Long userId;

    @NotBlank(message = "Текст комментария не может быть пустым")
    @Size(min = 1, max = 2000, message = "Комментарий от 1 до 2000 символов")
    private String content;
}
