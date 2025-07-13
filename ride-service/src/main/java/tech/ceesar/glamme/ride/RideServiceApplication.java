package tech.ceesar.glamme.ride;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "tech.ceesar.glamme")
public class RideServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RideServiceApplication.class, args);
    }
}
