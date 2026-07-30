package ru.practicum.ewm.error.decoder;

import feign.Feign;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ErrorDecoderConfig {

    @Bean
    public Feign.Builder feignBuilder() {
        return Feign.builder().errorDecoder(new CustomErrorDecoder());
    }

}