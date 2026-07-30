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
                .route("comment_service_route", r -> r.path(
                                "/public/events/*/comments",
                                "/users/*/events/*/comments/**",
                                "/admin/comments/**")
                        .uri("lb://comment-service"))
                .route("request_service_route", r -> r.path(
                                "/users/*/requests/**")
                        .uri("lb://request-service"))
                .route("user_service_route", r -> r.path(
                                "/admin/users/**")
                        .uri("lb://user-service"))
                .route("event_service_route", r -> r.path(
                                "/admin/events/**",
                                "/users/*/events/**",
                                "/admin/compilations/**",
                                "/compilations/**",
                                "/admin/categories/**",
                                "/categories/**",
                                "/events/**"
                        )
                        .uri("lb://event-service"))
                .build();
    }

}