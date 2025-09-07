package tech.ceesar.glamme.ride;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "tech.ceesar.glamme.ride",
        "tech.ceesar.glamme.common"
})
@EntityScan(basePackages = {
        "tech.ceesar.glamme.ride.entity",
        "tech.ceesar.glamme.common.entity"
})
@EnableJpaRepositories(basePackages = {
        "tech.ceesar.glamme.ride.repository",
        "tech.ceesar.glamme.common.repository"
})
@EnableAsync
@EnableScheduling
public class RideServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RideServiceApplication.class, args);
    }
}