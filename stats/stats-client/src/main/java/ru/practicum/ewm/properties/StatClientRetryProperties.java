package ru.practicum.ewm.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "stats.client.retry")
public class StatClientRetryProperties {
    private Long backOffPeriod = 5000L;
    private Integer maxAttempts = 1;
}