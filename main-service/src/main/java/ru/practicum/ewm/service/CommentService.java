package ru.practicum.ewm.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.dto.comment.CommentResponseDto;
import ru.practicum.ewm.dto.comment.CommentStatusUpdateRequest;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.dto.comment.UpdateCommentUserRequest;

public interface CommentService {
    Page<CommentResponseDto> getApprovedCommentsByEvent(Long eventId, Pageable pageable);

    CommentResponseDto addComment(Long userId, Long eventId, NewCommentDto dto);

    CommentResponseDto patchCommentById(UpdateCommentUserRequest dto);

    void removeCommentById(Long userId, Long eventId, Long commentId);

    CommentResponseDto updateCommentStatus(Long commentId, CommentStatusUpdateRequest request);

    void deleteAdminComment(Long commentId);

    Page<CommentResponseDto> getCommentsByEvent(Long eventId, String status, Pageable pageable);
}