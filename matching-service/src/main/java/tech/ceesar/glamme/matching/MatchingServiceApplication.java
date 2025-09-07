package tech.ceesar.glamme.matching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "tech.ceesar.glamme.matching",
        "tech.ceesar.glamme.common"
})
@EntityScan(basePackages = {
        "tech.ceesar.glamme.matching.entity",
        "tech.ceesar.glamme.common.entity"
})
@EnableJpaRepositories(basePackages = {
        "tech.ceesar.glamme.matching.repository",
        "tech.ceesar.glamme.common.repository"
})
@EnableAsync
@EnableScheduling
public class MatchingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatchingServiceApplication.class, args);
    }
}