package tech.ceesar.glamme.matching.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    @Bean
    public WebClient authWebClient(
            WebClient.Builder builder, @Value("${auth.service.url}") String authServiceUrl
    ) {
        return builder.baseUrl(authServiceUrl).build();
    }
}
