package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.comment.CommentResponseDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.dto.comment.UpdateCommentUserRequest;
import ru.practicum.ewm.service.CommentService;

@RestController
@RequestMapping("/users/{userId}/events/{eventId}/comments")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CommentPrivateController {
    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponseDto addComment(
            @PathVariable
                @Positive Long userId,
            @PathVariable
                @Positive Long eventId,
            @RequestBody
                @NotNull(message = "Добавляемый комментарий не может быть null")
                @Valid NewCommentDto dto
    ) {
        log.info("Создание нового комментария {} для события с ID: {} пользователем с ID: {}", dto, eventId, userId);
        return commentService.addComment(userId, eventId, dto);
    }

    @PatchMapping("/{commentId}")
    public CommentResponseDto patchCommentById(
            @PathVariable
                @Positive Long userId,
            @PathVariable
                @Positive Long eventId,
            @PathVariable
                @Positive Long commentId,
            @RequestBody
                @NotNull(message = "Данные для обновления комментария не могут быть null")
                @Valid UpdateCommentUserRequest dto
    ) {
        log.info("Обновление пользователем с ID: {} созданного им комментария с ID: {} к событию с ID: {}.",
                userId, commentId, eventId);
        dto.setId(commentId);
        dto.setUserId(userId);
        dto.setEventId(eventId);
        return commentService.patchCommentById(dto);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable
                @Positive Long userId,
            @PathVariable
                @Positive Long eventId,
            @PathVariable
                @Positive Long commentId
    ) {
        log.info("Удаление пользователем с ID: {} созданного им комментария с ID: {} к событию с ID: {}.",
                userId, commentId, eventId);
        commentService.removeCommentById(userId, eventId, commentId);
    }

}