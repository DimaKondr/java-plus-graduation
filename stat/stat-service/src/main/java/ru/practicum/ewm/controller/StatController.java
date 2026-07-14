package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.HitDto;
import ru.practicum.ewm.StatResponseDto;
import ru.practicum.ewm.service.StatService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class StatController {
    private final StatService statService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public HitDto createHit(@Valid @RequestBody HitDto hitDto) {
        log.info("POST /hit с телом: {}", hitDto);
        return statService.createHit(hitDto);
    }

    @GetMapping("/stats")
    public List<StatResponseDto> getStats(
            @RequestParam @Valid String start,
            @RequestParam @Valid String end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique) {

        log.info("GET /stats с параметрами: start={}, end={}, uris={}, unique={}",
                start, end, uris, unique);

        return statService.getStats(start, end, uris, unique);
    }

}