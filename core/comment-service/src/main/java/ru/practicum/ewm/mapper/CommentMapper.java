package ru.practicum.ewm.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.dto.comment.CommentResponseDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.CommentStatus;

import java.time.LocalDateTime;

@UtilityClass
public class CommentMapper {

    public Comment dtoToComment(
            NewCommentDto dto,
            CommentStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long authorId,
            Long eventId
    ) {
        return Comment.builder()
                .content(dto.getContent())
                .status(status)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .authorId(authorId)
                .eventId(eventId)
                .build();
    }

    public CommentResponseDto commentToResponseDto(Comment comment, UserShortDto user) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .eventId(comment.getEventId())
                .userId(comment.getAuthorId())
                .authorName(user.getName())
                .content(comment.getContent())
                .status(comment.getStatus().toString())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

}