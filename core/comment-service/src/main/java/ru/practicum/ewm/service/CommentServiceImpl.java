package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.constants.Constants;
import ru.practicum.ewm.dto.comment.CommentResponseDto;
import ru.practicum.ewm.dto.comment.CommentStatusUpdateRequest;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.dto.comment.UpdateCommentUserRequest;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.exception.CommentException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.mapper.CommentMapper;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.CommentStatus;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.service.integration.EventIntegrationService;
import ru.practicum.ewm.service.integration.UserIntegrationService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final EventIntegrationService eventIntegrationService;
    private final UserIntegrationService userIntegrationService;

    @Transactional
    @Override
    public CommentResponseDto addComment(Long userId, Long eventId, NewCommentDto dto) {
        UserShortDto author = userIntegrationService.getUserInfo(userId);
        EventFullDto event = eventIntegrationService.getEventInfo(eventId);

        Comment newComment = CommentMapper.dtoToComment(
                dto,
                CommentStatus.PENDING,
                LocalDateTime.now(),
                LocalDateTime.now(),
                author.getId(),
                event.getId()
        );

        Comment addedComment = commentRepository.save(newComment);
        log.info("Создан новый комментарий с ID: {}.", addedComment.getId());
        return CommentMapper.commentToResponseDto(addedComment,author);
    }

    @Transactional
    @Override
    public CommentResponseDto patchCommentById(UpdateCommentUserRequest dto) {
        Comment oldComment = commentRepository.findById(dto.getId()).orElseThrow(() -> new NotFoundException(
                "Обновление комментария. Комментарий с ID: " + dto.getId() + " не найден."));

        if (!oldComment.getAuthorId().equals(dto.getUserId())) {
            log.error("Обновление комментария. Переданное ID пользователя не совпадает с ID автора комментария.");
            throw new ValidationException("Обновление комментария. " +
                    "Переданное ID пользователя не совпадает с ID автора комментария.");
        } else if (!userIntegrationService.checkUserExisting(dto.getUserId())) {
            log.error("Обновление комментария. Пользователь с ID: {} не найден.", dto.getUserId());
            throw new NotFoundException("Обновление комментария. " +
                    "Пользователь с ID: " + dto.getUserId() + " не найден.");
        }

        if (!oldComment.getEventId().equals(dto.getEventId())) {
            log.error("Обновление комментария. Переданное ID события не совпадает с ID события комментария.");
            throw new ValidationException("Обновление комментария. " +
                    "Переданное ID события не совпадает с ID события комментария.");
        } else if (!eventIntegrationService.checkEventExisting(dto.getEventId())) {
            log.error("Обновление комментария. Событие с ID: {} не найдено.", dto.getEventId());
            throw new NotFoundException("Обновление комментария. Событие с ID: " + dto.getEventId() + " не найдено.");
        }

        LocalDateTime createdAt = oldComment.getCreatedAt();
        LocalDateTime now = LocalDateTime.now();
        if ((oldComment.getStatus().equals(CommentStatus.APPROVED)
                || oldComment.getStatus().equals(CommentStatus.PENDING))
                && createdAt.isBefore(now.minusHours(24L))) {
            log.error("Обновление комментария. C момента публикации комментария с ID: {} прошло более 24 часов. " +
                    "Создание: {}. Попытка изменения: {}. Редактирование невозможно.",
                    oldComment.getId(), createdAt.format(Constants.FORMATTER), now.format(Constants.FORMATTER));
            throw new ValidationException("Обновление комментария. " +
                    "C момента публикации комментария прошло более 24 часов. Редактирование невозможно.");
        }

        oldComment.setContent(dto.getContent());
        oldComment.setUpdatedAt(LocalDateTime.now());

        Comment patchedComment = commentRepository.save(oldComment);
        log.info("Данные комментария с ID: {} обновлены.", oldComment.getId());

        UserShortDto author = userIntegrationService.getUserInfo(dto.getUserId());

        return CommentMapper.commentToResponseDto(patchedComment, author);
    }

    @Transactional
    @Override
    public void removeCommentById(Long userId, Long eventId, Long commentId) {
        Comment removedComment = commentRepository.findById(commentId).orElseThrow(() -> new NotFoundException(
                "Удаление комментария. Комментарий с ID: " + commentId + " не найден."));

        if (!removedComment.getAuthorId().equals(userId)) {
            log.error("Удаление комментария. Переданное ID пользователя не совпадает с ID автора комментария.");
            throw new ValidationException("Удаление комментария. " +
                    "Переданное ID пользователя не совпадает с ID автора комментария.");
        } else if (!userIntegrationService.checkUserExisting(userId)) {
            log.error("Удаление комментария. Пользователь с ID: {} не найден.", userId);
            throw new NotFoundException("Удаление комментария. " +
                    "Пользователь с ID: " + userId + " не найден.");
        }

        if (!removedComment.getEventId().equals(eventId)) {
            log.error("Удаление комментария. Переданное ID события не совпадает с ID события комментария.");
            throw new ValidationException("Удаление комментария. " +
                    "Переданное ID события не совпадает с ID события комментария.");
        } else if (!eventIntegrationService.checkEventExisting(eventId)) {
            log.error("Удаление комментария. Событие с ID: {} не найдено.", eventId);
            throw new NotFoundException("Удаление комментария. Событие с ID: " + eventId + " не найдено.");
        }

        commentRepository.deleteById(commentId);
        log.info("Комментарий с ID: {} удален.", commentId);
    }

    @Override
    public Page<CommentResponseDto> getApprovedCommentsByEvent(Long eventId, Pageable pageable) {
        log.debug("Получение подтвержденных комментариев для event: {}", eventId);
        Page<Comment> comments = commentRepository.findByEventIdAndStatus(eventId, CommentStatus.APPROVED, pageable);

        Map<Long, UserShortDto> usersInfoMap = getUsersInfoMap(comments);

        return comments.map(
                (comment) -> CommentMapper.commentToResponseDto(comment, usersInfoMap.get(comment.getAuthorId()))
        );
    }

    @Transactional
    @Override
    public CommentResponseDto updateCommentStatus(Long commentId, CommentStatusUpdateRequest request) {
        log.info("Admin: изменить статус комментария с id={} на status - {}", commentId, request.getStatus());
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий с ID=%d не найден", commentId)));

        CommentStatus newStatus;
        try {
            newStatus = CommentStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CommentException("Недопустимый статус: " + request.getStatus());
        }

        if (comment.getStatus() == newStatus) {
            throw new CommentException(String.format(
                    "Комментарий с ID=%d уже имеет статус '%s'", commentId, newStatus));
        }

        comment.setStatus(newStatus);
        comment.setUpdatedAt(LocalDateTime.now());

        Comment updated = commentRepository.save(comment);
        log.info("Admin: статус комментария с id={} изменен на {}", commentId, newStatus);

        UserShortDto author = userIntegrationService.getUserInfo(comment.getAuthorId());

        return CommentMapper.commentToResponseDto(updated, author);
    }

    @Override
    public Page<CommentResponseDto> getCommentsByEvent(Long eventId, String status, Pageable pageable) {
        log.debug("Admin: получить комментарии по событию eventId= {}, status: {}", eventId, status);

        if (!eventIntegrationService.checkEventExisting(eventId)) {
            throw new NotFoundException("Событие с ID: " + eventId + " не найдено");
        }

        if (status != null && !status.isBlank()) {
            try {
                CommentStatus commentStatus = CommentStatus.valueOf(status.toUpperCase());
                Page<Comment> comments = commentRepository.findByEventIdAndStatus(eventId, commentStatus, pageable);

                Map<Long, UserShortDto> usersInfoMap = getUsersInfoMap(comments);

                return comments.map(
                        (comment) -> CommentMapper.commentToResponseDto(
                                comment, usersInfoMap.get(comment.getAuthorId())
                        )
                );
            } catch (IllegalArgumentException e) {
                log.warn("Некорректный статус: {}", status);
                throw new ValidationException("Некорректный статус. Допустимые значения: PENDING, APPROVED, REJECTED");
            }
        }

        Page<Comment> comments = commentRepository.findByEventId(eventId, pageable);
        Map<Long, UserShortDto> usersInfoMap = getUsersInfoMap(comments);

        return comments.map(
                (comment) -> CommentMapper.commentToResponseDto(
                        comment, usersInfoMap.get(comment.getAuthorId())
                )
        );
    }

    @Override
    @Transactional
    public void deleteAdminComment(Long commentId) {
        log.info("Admin: удалить комментарий с id: {}", commentId);

        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Комментарий с ID: " + commentId + " не найден");
        }

        commentRepository.deleteById(commentId);
        log.info("Admin: comment deleted, id: {}", commentId);
    }

    private Map<Long, UserShortDto> getUsersInfoMap(Page<Comment> comments) {
        List<Long> usersIds = new ArrayList<>();
        for (Comment comment : comments) {
            usersIds.add(comment.getAuthorId());
        }

        return userIntegrationService.getInfoOfUsers(usersIds).stream()
                .collect(Collectors.toMap(UserShortDto::getId, Function.identity()));
    }

}