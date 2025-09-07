package tech.ceesar.glamme.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "tech.ceesar.glamme.booking",
        "tech.ceesar.glamme.common"
})
@EntityScan(basePackages = {
        "tech.ceesar.glamme.booking.entity",
        "tech.ceesar.glamme.common.entity"
})
@EnableJpaRepositories(basePackages = {
        "tech.ceesar.glamme.booking.repository",
        "tech.ceesar.glamme.common.repository"
})
@EnableAsync
@EnableScheduling
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}