package tech.ceesar.glamme.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventBridgeConsumerService {

    private final EventBridgeClient eventBridgeClient;
    private final ObjectMapper objectMapper;
    private final Map<String, Consumer<EventMessage>> eventHandlers = new ConcurrentHashMap<>();

    /**
     * Register event handler for specific event type
     */
    public void registerHandler(String eventType, Consumer<EventMessage> handler) {
        eventHandlers.put(eventType, handler);
        log.info("Registered event handler for type: {}", eventType);
    }

    /**
     * Process incoming event
     */
    public void processEvent(String eventType, String source, String detail) {
        try {
            Consumer<EventMessage> handler = eventHandlers.get(eventType);
            if (handler != null) {
                JsonNode detailNode = objectMapper.readTree(detail);
                EventMessage eventMessage = new EventMessage(eventType, source, detailNode);
                handler.accept(eventMessage);
                log.debug("Processed event: {} from source: {}", eventType, source);
            } else {
                log.warn("No handler registered for event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing event: {} from source: {}", eventType, source, e);
        }
    }

    /**
     * Publish event to EventBridge
     */
    public void publishEvent(String eventBusName, String source, String detailType, Object detail) {
        try {
            String detailJson = objectMapper.writeValueAsString(detail);

            PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                    .eventBusName(eventBusName)
                    .source(source)
                    .detailType(detailType)
                    .detail(detailJson)
                    .build();

            PutEventsRequest request = PutEventsRequest.builder()
                    .entries(entry)
                    .build();

            var response = eventBridgeClient.putEvents(request);

            if (response.failedEntryCount() > 0) {
                log.error("Failed to publish event: {}", response.entries().get(0).errorMessage());
            } else {
                log.debug("Published event: {} to bus: {}", detailType, eventBusName);
            }

        } catch (Exception e) {
            log.error("Error publishing event: {}", detailType, e);
        }
    }

    /**
     * Get all registered event types
     */
    public java.util.Set<String> getRegisteredEventTypes() {
        return eventHandlers.keySet();
    }

    /**
     * Unregister event handler
     */
    public void unregisterHandler(String eventType) {
        eventHandlers.remove(eventType);
        log.info("Unregistered event handler for type: {}", eventType);
    }

    /**
     * Event message wrapper
     */
    public static class EventMessage {
        private final String eventType;
        private final String source;
        private final JsonNode detail;

        public EventMessage(String eventType, String source, JsonNode detail) {
            this.eventType = eventType;
            this.source = source;
            this.detail = detail;
        }

        public String getEventType() { return eventType; }
        public String getSource() { return source; }
        public JsonNode getDetail() { return detail; }

        public String getDetailAsString(String field) {
            JsonNode node = detail.get(field);
            return node != null ? node.asText() : null;
        }

        public Integer getDetailAsInt(String field) {
            JsonNode node = detail.get(field);
            return node != null ? node.asInt() : null;
        }

        public Long getDetailAsLong(String field) {
            JsonNode node = detail.get(field);
            return node != null ? node.asLong() : null;
        }
    }
}
