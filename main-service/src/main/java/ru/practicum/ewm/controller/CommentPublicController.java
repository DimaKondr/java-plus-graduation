package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.comment.CommentResponseDto;
import ru.practicum.ewm.service.CommentService;

@Slf4j
@RestController
@RequestMapping("/public/events/{eventId}/comments")
@RequiredArgsConstructor
public class CommentPublicController {

    private final CommentService commentService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<CommentResponseDto> getApprovedCommentsByEvent(
            @PathVariable Long eventId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Получение ОПУБЛИКОВАННЫХ комментариев для event: {}", eventId);

        return commentService.getApprovedCommentsByEvent(eventId, pageable);
    }
}
