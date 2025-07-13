package tech.ceesar.glamme.ride.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;

@Configuration
public class LyftConfig {
    @Value("${external.lyft.clientId}") private String clientId;
    @Value("${external.lyft.clientSecret}") private String clientSecret;

    @Bean("LYFT")
    public WebClient lyftClient() {
        String token = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());
        return WebClient.builder()
                .baseUrl("https://api.lyft.com/v1")
                .defaultHeader("Authorization", "Basic " + token)
                .build();
    }
}
