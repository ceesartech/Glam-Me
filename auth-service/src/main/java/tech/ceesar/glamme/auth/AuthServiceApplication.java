package tech.ceesar.glamme.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import tech.ceesar.glamme.auth.config.CognitoConfig;

@SpringBootApplication(scanBasePackages = "tech.ceesar.glamme")
@EnableConfigurationProperties(CognitoConfig.class)
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
