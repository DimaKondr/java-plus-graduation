package ru.practicum.ewm.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.HitDto;
import ru.practicum.ewm.StatResponseDto;
import ru.practicum.ewm.service.StatService;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatControllerTest {

    @Mock
    private StatService statService;

    @InjectMocks
    private StatController statController;

    private HitDto hitDto;
    private StatResponseDto statResponseDto;

    @BeforeEach
    void setUp() {
        hitDto = new HitDto();
        hitDto.setId(1L);
        hitDto.setApp("test-app");
        hitDto.setUri("/test");
        hitDto.setIp("127.0.0.1");
        hitDto.setTimestamp("2024-01-01 12:00:00");

        statResponseDto = new StatResponseDto("test-app", "/test", 5L);
    }

    @Test
    void testCreateHit() {
        when(statService.createHit(any(HitDto.class))).thenReturn(hitDto);

        HitDto result = statController.createHit(hitDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test-app", result.getApp());
        assertEquals("/test", result.getUri());
        assertEquals("127.0.0.1", result.getIp());
        assertEquals("2024-01-01 12:00:00", result.getTimestamp());

        verify(statService, times(1)).createHit(hitDto);
    }

    @Test
    void testGtStatsWithUris() {
        List<StatResponseDto> expectedStats = Arrays.asList(statResponseDto);
        when(statService.getStats(anyString(), anyString(), any(), anyBoolean()))
                .thenReturn(expectedStats);

        List<StatResponseDto> result = statController.getStats(
                "2024-01-01 00:00:00",
                "2024-01-01 23:59:59",
                Arrays.asList("/test"),
                false
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test-app", result.get(0).getApp());
        assertEquals("/test", result.get(0).getUri());
        assertEquals(5L, result.get(0).getHits());

        verify(statService, times(1)).getStats(
                "2024-01-01 00:00:00",
                "2024-01-01 23:59:59",
                Arrays.asList("/test"),
                false
        );
    }

    @Test
    void testGetStatsWithoutUris() {
        List<StatResponseDto> expectedStats = Arrays.asList(statResponseDto);
        when(statService.getStats(anyString(), anyString(), isNull(), anyBoolean()))
                .thenReturn(expectedStats);

        List<StatResponseDto> result = statController.getStats(
                "2024-01-01 00:00:00",
                "2024-01-01 23:59:59",
                null,
                true
        );

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(statService, times(1)).getStats(
                "2024-01-01 00:00:00",
                "2024-01-01 23:59:59",
                null,
                true
        );
    }

    @Test
    void testGetStatsWithUniqueTrue() {
        List<StatResponseDto> expectedStats = Arrays.asList(statResponseDto);
        when(statService.getStats(anyString(), anyString(), any(), eq(true)))
                .thenReturn(expectedStats);

        List<StatResponseDto> result = statController.getStats(
                "2024-01-01 00:00:00",
                "2024-01-01 23:59:59",
                Arrays.asList("/test"),
                true
        );

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(statService, times(1)).getStats(
                "2024-01-01 00:00:00",
                "2024-01-01 23:59:59",
                Arrays.asList("/test"),
                true
        );
    }
}