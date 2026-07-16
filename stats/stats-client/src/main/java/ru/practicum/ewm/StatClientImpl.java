package ru.practicum.ewm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.ewm.exception.StatsServerUnavailable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
public class StatClientImpl implements StatClient {
    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final String statsServiceId;

    public StatClientImpl(
            RestClient.Builder builder,
            DiscoveryClient discoveryClient,
            RetryTemplate retryTemplate,
            String statsServiceId
    ) {
        this.restClient = builder
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.discoveryClient = discoveryClient;
        this.statsServiceId = statsServiceId;
        this.retryTemplate = retryTemplate;
    }

    @Override
    public HitDto postHit(HitDto dto) {
        try {
            URI targetUri = makeUri("/hit");

            return restClient.post()
                    .uri(targetUri)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .body(dto)
                    .retrieve()
                    .body(HitDto.class);
        } catch (Exception e) {
            log.error("Неудачная попытка добавления записи в сервис статистики. Запись: {}", dto);
            return new HitDto();
        }
    }

    @Override
    public List<StatResponseDto> getStats(StatRequestParamDto dto) {
        try {
            URI targetUri = makeUri("/stats");

            return restClient.get()
                    .uri(uriBuilder -> UriComponentsBuilder.fromUri(targetUri)
                            .queryParam("start", dto.getStart())
                            .queryParam("end", dto.getEnd())
                            .queryParam("uris", dto.getUris())
                            .queryParam("unique", dto.getUnique())
                            .build()
                            .toUri())
                    .accept(APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<StatResponseDto>>() {
                    });
        } catch (Exception e) {
            log.error("Неудачная попытка получения данных статистики из сервиса статистики. " +
                    "Параметры запроса: {}", dto);
            return new ArrayList<>();
        }
    }

    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(cxt -> getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    private ServiceInstance getInstance() {
        try {
            return discoveryClient
                    .getInstances(statsServiceId)
                    .getFirst();
        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId,
                    exception
            );
        }
    }

}