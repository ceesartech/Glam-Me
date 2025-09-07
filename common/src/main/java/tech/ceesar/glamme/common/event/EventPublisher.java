package tech.ceesar.glamme.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final EventBridgeClient eventBridgeClient;

    @Value("${aws.eventbridge.bus-name:glamme-bus}")
    private String eventBusName;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void publishEvent(String detailType, Object detail, Map<String, String> additionalSourceAttributes) {
        try {
            String detailJson = objectMapper.writeValueAsString(detail);
            String source = "glamme." + getServiceName();

            PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                    .eventBusName(eventBusName)
                    .source(source)
                    .detailType(detailType)
                    .detail(detailJson)
                    .time(Instant.now())
                    .build();

            PutEventsRequest request = PutEventsRequest.builder()
                    .entries(entry)
                    .build();

            var response = eventBridgeClient.putEvents(request);

            if (response.failedEntryCount() > 0) {
                log.error("Failed to publish event: {}", response.entries().get(0).errorMessage());
            } else {
                log.debug("Successfully published event: {} with detail: {}", detailType, detailJson);
            }

        } catch (Exception e) {
            log.error("Error publishing event: {}", detailType, e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    public void publishEvent(String detailType, Object detail) {
        publishEvent(detailType, detail, Map.of());
    }

    private String getServiceName() {
        // Extract service name from the current context
        String serviceName = System.getenv("SERVICE_NAME");
        return serviceName != null ? serviceName : "unknown-service";
    }
}
