package tech.ceesar.glamme.matching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "tech.ceesar.glamme")
public class MatchingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MatchingServiceApplication.class, args);
    }
}
