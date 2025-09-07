package tech.ceesar.glamme.image;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "tech.ceesar.glamme")
@EnableScheduling
public class ImageServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImageServiceApplication.class, args);
    }
}
