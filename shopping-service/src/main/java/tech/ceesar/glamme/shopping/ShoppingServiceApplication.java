package tech.ceesar.glamme.shopping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "tech.ceesar.glamme")
public class ShoppingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShoppingServiceApplication.class, args);
    }
}
