package ru.practicum.ewm.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.StatResponseDto;
import ru.practicum.ewm.model.Hit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HitRepositoryIntegrationTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Autowired
    private HitRepository hitRepository;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        hitRepository.deleteAll();
        baseTime = LocalDateTime.parse("2024-01-01 12:00:00", FORMATTER);
    }

    @Test
    void testFindUniqueStats() {
        Hit hit1 = createHit("app1", "/test1", "192.168.1.1", baseTime);
        Hit hit2 = createHit("app1", "/test1", "192.168.1.2", baseTime.plusMinutes(1));
        Hit hit3 = createHit("app1", "/test1", "192.168.1.1", baseTime.plusMinutes(2));
        Hit hit4 = createHit("app2", "/test2", "192.168.1.3", baseTime.plusMinutes(3));

        hitRepository.saveAll(Arrays.asList(hit1, hit2, hit3, hit4));

        List<StatResponseDto> stats = hitRepository.findUniqueStats(
                baseTime.minusMinutes(1),
                baseTime.plusMinutes(5),
                Arrays.asList("/test1", "/test2")
        );

        assertEquals(2, stats.size());

        StatResponseDto stat1 = stats.get(0);
        assertEquals("app1", stat1.getApp());
        assertEquals("/test1", stat1.getUri());
        assertEquals(2L, stat1.getHits());

        StatResponseDto stat2 = stats.get(1);
        assertEquals("app2", stat2.getApp());
        assertEquals("/test2", stat2.getUri());
        assertEquals(1L, stat2.getHits());
    }

    @Test
    void testFindNonUniqueStats() {
        Hit hit1 = createHit("app1", "/test1", "192.168.1.1", baseTime);
        Hit hit2 = createHit("app1", "/test1", "192.168.1.1", baseTime.plusMinutes(1));
        Hit hit3 = createHit("app1", "/test1", "192.168.1.2", baseTime.plusMinutes(2));

        hitRepository.saveAll(Arrays.asList(hit1, hit2, hit3));

        List<StatResponseDto> stats = hitRepository.findNonUniqueStats(
                baseTime.minusMinutes(1),
                baseTime.plusMinutes(5),
                Arrays.asList("/test1")
        );

        assertEquals(1, stats.size());
        assertEquals(3L, stats.get(0).getHits());
    }

    @Test
    void testFindUniqueStatsWithEmptyUriList() {
        Hit hit1 = createHit("app1", "/test1", "192.168.1.1", baseTime);
        Hit hit2 = createHit("app2", "/test2", "192.168.1.2", baseTime.plusMinutes(1));

        hitRepository.saveAll(Arrays.asList(hit1, hit2));

        List<StatResponseDto> stats = hitRepository.findUniqueStats(
                baseTime.minusMinutes(1),
                baseTime.plusMinutes(5),
                null
        );

        assertEquals(2, stats.size());
    }

    @Test
    void testFindUniqueStatsWithDateRange() {
        Hit hit1 = createHit("app1", "/test1", "192.168.1.1", baseTime);
        Hit hit2 = createHit("app1", "/test1", "192.168.1.2", baseTime.plusDays(1));

        hitRepository.saveAll(Arrays.asList(hit1, hit2));

        List<StatResponseDto> stats = hitRepository.findUniqueStats(
                baseTime.minusMinutes(1),
                baseTime.plusMinutes(30),
                Arrays.asList("/test1")
        );

        assertEquals(1, stats.size());
        assertEquals(1L, stats.get(0).getHits());
    }

    private Hit createHit(String app, String uri, String ip, LocalDateTime timestamp) {
        Hit hit = new Hit();
        hit.setApp(app);
        hit.setUri(uri);
        hit.setIp(ip);
        hit.setTimestamp(timestamp);
        return hit;
    }
}
