package ru.practicum.ewm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.HitDto;
import ru.practicum.ewm.StatResponseDto;
import ru.practicum.ewm.mapper.HitMapper;
import ru.practicum.ewm.model.Hit;
import ru.practicum.ewm.repository.HitRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatServiceImplTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Mock
    private HitRepository hitRepository;
    @Mock
    private HitMapper hitMapper;
    @InjectMocks
    private StatServiceImpl statService;
    private HitDto hitDto;
    private Hit hit;
    private StatResponseDto statResponseDto;

    @BeforeEach
    void setUp() {
        hitDto = new HitDto();
        hitDto.setId(1L);
        hitDto.setApp("test-app");
        hitDto.setUri("/test");
        hitDto.setIp("127.0.0.1");
        hitDto.setTimestamp("2024-01-01 12:00:00");

        hit = new Hit();
        hit.setId(1L);
        hit.setApp("test-app");
        hit.setUri("/test");
        hit.setIp("127.0.0.1");
        hit.setTimestamp(LocalDateTime.parse("2024-01-01 12:00:00", FORMATTER));

        statResponseDto = new StatResponseDto("test-app", "/test", 5L);
    }

    @Test
    void testCreateHit() {
        when(hitMapper.toEntity(hitDto)).thenReturn(hit);
        when(hitRepository.save(hit)).thenReturn(hit);
        when(hitMapper.toDto(hit)).thenReturn(hitDto);

        HitDto result = statService.createHit(hitDto);

        assertNotNull(result);
        assertEquals(hitDto.getId(), result.getId());
        assertEquals(hitDto.getApp(), result.getApp());
        assertEquals(hitDto.getUri(), result.getUri());
        assertEquals(hitDto.getIp(), result.getIp());

        verify(hitMapper, times(1)).toEntity(hitDto);
        verify(hitRepository, times(1)).save(hit);
        verify(hitMapper, times(1)).toDto(hit);
    }

    @Test
    void testGetStatsWithUniqueTrue() {
        String start = "2024-01-01 00:00:00";
        String end = "2024-01-01 23:59:59";
        List<String> uris = Arrays.asList("/test");
        Boolean unique = true;

        List<StatResponseDto> expectedStats = Arrays.asList(statResponseDto);

        when(hitRepository.findUniqueStats(any(LocalDateTime.class), any(LocalDateTime.class), eq(uris)))
                .thenReturn(expectedStats);

        List<StatResponseDto> result = statService.getStats(start, end, uris, unique);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(statResponseDto, result.get(0));
        verify(hitRepository, times(1)).findUniqueStats(any(), any(), eq(uris));
        verify(hitRepository, never()).findNonUniqueStats(any(), any(), any());
    }

    @Test
    void testGetStatsWithUniqueFalse() {
        String start = "2024-01-01 00:00:00";
        String end = "2024-01-01 23:59:59";
        List<String> uris = Arrays.asList("/test");
        Boolean unique = false;

        List<StatResponseDto> expectedStats = Arrays.asList(statResponseDto);

        when(hitRepository.findNonUniqueStats(any(LocalDateTime.class), any(LocalDateTime.class), eq(uris)))
                .thenReturn(expectedStats);

        List<StatResponseDto> result = statService.getStats(start, end, uris, unique);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(hitRepository, times(1)).findNonUniqueStats(any(), any(), eq(uris));
        verify(hitRepository, never()).findUniqueStats(any(), any(), any());
    }

    @Test
    void testGetStatsWithNullUris() {
        String start = "2024-01-01 00:00:00";
        String end = "2024-01-01 23:59:59";
        List<String> uris = null;
        Boolean unique = true;

        when(hitRepository.findUniqueStats(any(), any(), isNull())).thenReturn(Arrays.asList());

        statService.getStats(start, end, uris, unique);

        verify(hitRepository, times(1)).findUniqueStats(any(), any(), isNull());
    }

    @Test
    void testGetStatsWithStartAfterEnd() {
        String start = "2024-01-02 00:00:00";
        String end = "2024-01-01 23:59:59";
        List<String> uris = Arrays.asList("/test");
        Boolean unique = false;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> statService.getStats(start, end, uris, unique));

        assertEquals("Дата начала не может быть позже даты окончания", exception.getMessage());
    }
}