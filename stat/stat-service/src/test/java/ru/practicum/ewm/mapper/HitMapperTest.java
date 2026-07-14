package ru.practicum.ewm.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.ewm.HitDto;
import ru.practicum.ewm.model.Hit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HitMapperTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private HitMapper hitMapper;

    @BeforeEach
    void setUp() {
        hitMapper = new HitMapper();
    }

    @Test
    void testToEntity() {
        HitDto dto = new HitDto();
        dto.setApp("test-app");
        dto.setUri("/test");
        dto.setIp("127.0.0.1");
        dto.setTimestamp("2024-01-01 12:00:00");

        Hit hit = hitMapper.toEntity(dto);

        assertNotNull(hit);
        assertEquals(null, hit.getId());
        assertEquals(dto.getApp(), hit.getApp());
        assertEquals(dto.getUri(), hit.getUri());
        assertEquals(dto.getIp(), hit.getIp());
        assertEquals(LocalDateTime.parse(dto.getTimestamp(), FORMATTER), hit.getTimestamp());
    }

    @Test
    void testToDto() {
        Hit hit = new Hit();
        hit.setId(1L);
        hit.setApp("test-app");
        hit.setUri("/test");
        hit.setIp("127.0.0.1");
        hit.setTimestamp(LocalDateTime.parse("2024-01-01 12:00:00", FORMATTER));

        HitDto dto = hitMapper.toDto(hit);

        assertNotNull(dto);
        assertEquals(hit.getId(), dto.getId());
        assertEquals(hit.getApp(), dto.getApp());
        assertEquals(hit.getUri(), dto.getUri());
        assertEquals(hit.getIp(), dto.getIp());
        assertEquals(hit.getTimestamp().format(FORMATTER), dto.getTimestamp());
    }
}