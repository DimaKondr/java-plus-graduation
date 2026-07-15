package ru.practicum.ewm;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayServerConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("main_service_route", r -> r.path(
                                "/admin/**",
                                "/users/**",
                                "/events/**",
                                "/public/**",
                                "/categories/**",
                                "/compilations/**"
                        )
                        .uri("lb://MAIN-SERVICE"))
                .build();
    }

}