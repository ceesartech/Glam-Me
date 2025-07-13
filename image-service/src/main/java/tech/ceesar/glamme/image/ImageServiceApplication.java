package tech.ceesar.glamme.image;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "tech.ceesar.glamme")
public class ImageServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImageServiceApplication.class, args);
    }
}
