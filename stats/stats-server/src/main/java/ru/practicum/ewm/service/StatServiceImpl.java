package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.HitDto;
import ru.practicum.ewm.StatResponseDto;
import ru.practicum.ewm.mapper.HitMapper;
import ru.practicum.ewm.model.Hit;
import ru.practicum.ewm.repository.HitRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatServiceImpl implements StatService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final HitRepository hitRepository;
    private final HitMapper hitMapper;

    @Override
    @Transactional
    public HitDto createHit(HitDto hitDto) {
        log.info("Сохранение информации о запросе: {}", hitDto);
        Hit hit = hitMapper.toEntity(hitDto);
        Hit savedHit = hitRepository.save(hit);
        return hitMapper.toDto(savedHit);
    }

    @Override
    public List<StatResponseDto> getStats(String start, String end, List<String> uris, Boolean unique) {
        log.info("ПОИСК СТАТИСТИКИ: uris = {}, start = {}, end = {}", uris, start, end);
        LocalDateTime startTime = LocalDateTime.parse(start, FORMATTER);
        LocalDateTime endTime = LocalDateTime.parse(end, FORMATTER);

        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        List<StatResponseDto> stats;
        if (Boolean.TRUE.equals(unique)) {
            stats = hitRepository.findUniqueStats(startTime, endTime, uris);
        } else {
            stats = hitRepository.findNonUniqueStats(startTime, endTime, uris);
        }

        log.info("Получена статистика: {} записей", stats.size());
        log.info("РЕЗУЛЬТАТ ИЗ БД: {}", stats);
        return stats;
    }

}