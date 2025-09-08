package tech.ceesar.glamme.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import tech.ceesar.glamme.common.event.GlammeEventConsumer;

import jakarta.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor
public class EventConfig {

    private final GlammeEventConsumer glammeEventConsumer;

    @PostConstruct
    public void initializeEventHandlers() {
        glammeEventConsumer.registerHandlers();
    }
}
