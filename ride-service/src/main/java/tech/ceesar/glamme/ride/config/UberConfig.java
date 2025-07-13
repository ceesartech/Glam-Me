package tech.ceesar.glamme.ride.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class UberConfig {
    @Value("${external.uber.serverToken}")
    private String serverToken;

    @Bean("UBER")
    public WebClient uberClient() {
        return WebClient.builder()
                .baseUrl("https://api.uber.com/v1.2")
                .defaultHeader("Authorization", "Token " + serverToken)
                .build();
    }
}
