package ru.practicum.ewm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import ru.practicum.ewm.StatClient;
import ru.practicum.ewm.StatClientImpl;
import ru.practicum.ewm.properties.StatClientRetryProperties;

@Configuration
@EnableConfigurationProperties(StatClientRetryProperties.class)
public class StatClientConfig {

    @Bean
    @Profile("!test")
    public RestClient.Builder prodRestClientBuilder(RestClientBuilderConfigurer configurer) {
        return configurer.configure(RestClient.builder());
    }

    @Bean
    @Profile("test")
    public RestClient.Builder testRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RetryTemplate retryTemplate(StatClientRetryProperties retryProperties) {
        return RetryTemplate.builder()
                .maxAttempts(retryProperties.getMaxAttempts())
                .fixedBackoff(retryProperties.getBackOffPeriod())
                .build();
    }

    @Bean
    public StatClient statClient(
            RestClient.Builder builder,
            DiscoveryClient discoveryClient,
            RetryTemplate retryTemplate,
            @Value("${client.name:stats-server}") String statsServiceId
    ) {
        return new StatClientImpl(builder, discoveryClient, retryTemplate, statsServiceId);
    }

}