package ru.practicum.ewm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.StatClient;
import ru.practicum.ewm.StatClientImpl;

@Configuration
public class StatClientConfig {

    @Bean
    public StatClient statClient(
            DiscoveryClient discoveryClient,
            @Value("${client.name:stats-server}") String statsServiceId
    ) {
        return new StatClientImpl(discoveryClient, statsServiceId);
    }

}