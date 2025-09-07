package tech.ceesar.glamme.common.service.aws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.appconfig.AppConfigClient;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionResponse;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppConfigService {

    private final AppConfigClient appConfigClient;
    private final AppConfigDataClient appConfigDataClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${aws.appconfig.application-id:}")
    private String applicationId;

    @Value("${aws.appconfig.environment-id:}")
    private String environmentId;

    @Value("${aws.appconfig.configuration-profile-id:}")
    private String configurationProfileId;

    // Cache for configuration sessions
    private final Map<String, String> sessionTokens = new ConcurrentHashMap<>();

    /**
     * Get feature flag value
     */
    public boolean getFeatureFlag(String flagName) {
        return getBooleanConfigValue("features." + flagName, false);
    }

    /**
     * Get feature flag value with default
     */
    public boolean getFeatureFlag(String flagName, boolean defaultValue) {
        return getBooleanConfigValue("features." + flagName, defaultValue);
    }

    /**
     * Get string configuration value
     */
    public String getStringConfig(String key) {
        return getStringConfigValue(key, null);
    }

    /**
     * Get string configuration value with default
     */
    public String getStringConfig(String key, String defaultValue) {
        return getStringConfigValue(key, defaultValue);
    }

    /**
     * Get integer configuration value
     */
    public int getIntConfig(String key, int defaultValue) {
        return getIntConfigValue(key, defaultValue);
    }

    /**
     * Get boolean configuration value
     */
    public boolean getBooleanConfig(String key, boolean defaultValue) {
        return getBooleanConfigValue(key, defaultValue);
    }

    /**
     * Get double configuration value
     */
    public double getDoubleConfig(String key, double defaultValue) {
        return getDoubleConfigValue(key, defaultValue);
    }

    /**
     * Refresh configuration cache
     */
    public void refreshConfiguration() {
        sessionTokens.clear();
        log.info("AppConfig configuration cache cleared, will refresh on next access");
    }

    /**
     * Check if a service is enabled
     */
    public boolean isServiceEnabled(String serviceName) {
        return getFeatureFlag("services." + serviceName + ".enabled", true);
    }

    /**
     * Get service configuration
     */
    public JsonNode getServiceConfig(String serviceName) {
        String configPath = "services." + serviceName;
        try {
            JsonNode config = getConfiguration();
            return config.at("/" + configPath.replace(".", "/"));
        } catch (Exception e) {
            log.warn("Failed to get service config for: {}", serviceName, e);
            return null;
        }
    }

    /**
     * Get database configuration
     */
    public JsonNode getDatabaseConfig() {
        return getServiceConfig("database");
    }

    /**
     * Get cache configuration
     */
    public JsonNode getCacheConfig() {
        return getServiceConfig("cache");
    }

    /**
     * Get messaging configuration
     */
    public JsonNode getMessagingConfig() {
        return getServiceConfig("messaging");
    }

    /**
     * Get AI/ML configuration
     */
    public JsonNode getAiConfig() {
        return getServiceConfig("ai");
    }

    /**
     * Get payment configuration
     */
    public JsonNode getPaymentConfig() {
        return getServiceConfig("payment");
    }

    // Private helper methods

    private JsonNode getConfiguration() {
        try {
            String sessionToken = getOrCreateSessionToken();

            GetLatestConfigurationRequest request = GetLatestConfigurationRequest.builder()
                    .configurationToken(sessionToken)
                    .build();

            GetLatestConfigurationResponse response = appConfigDataClient.getLatestConfiguration(request);

            // Update session token for next request
            sessionTokens.put("default", response.nextPollConfigurationToken());

            if (response.configuration().asByteArray().length > 0) {
                byte[] configBytes = response.configuration().asByteArray();
                String configJson = new String(configBytes, StandardCharsets.UTF_8);
                return objectMapper.readTree(configJson);
            } else {
                // Return empty config if no configuration available
                return objectMapper.createObjectNode();
            }

        } catch (Exception e) {
            log.error("Failed to get AppConfig configuration", e);
            return objectMapper.createObjectNode();
        }
    }

    private String getOrCreateSessionToken() {
        return sessionTokens.computeIfAbsent("default", k -> {
            try {
                StartConfigurationSessionRequest request = StartConfigurationSessionRequest.builder()
                        .applicationIdentifier(applicationId)
                        .environmentIdentifier(environmentId)
                        .configurationProfileIdentifier(configurationProfileId)
                        .build();

                StartConfigurationSessionResponse response = appConfigDataClient.startConfigurationSession(request);
                return response.initialConfigurationToken();

            } catch (Exception e) {
                log.error("Failed to start AppConfig session", e);
                throw new RuntimeException("Failed to start AppConfig session", e);
            }
        });
    }

    private String getStringConfigValue(String key, String defaultValue) {
        try {
            JsonNode config = getConfiguration();
            JsonNode value = config.at("/" + key.replace(".", "/"));
            return value.isMissingNode() ? defaultValue : value.asText(defaultValue);
        } catch (Exception e) {
            log.warn("Failed to get config value for key: {}", key, e);
            return defaultValue;
        }
    }

    private int getIntConfigValue(String key, int defaultValue) {
        try {
            JsonNode config = getConfiguration();
            JsonNode value = config.at("/" + key.replace(".", "/"));
            return value.isMissingNode() ? defaultValue : value.asInt(defaultValue);
        } catch (Exception e) {
            log.warn("Failed to get int config value for key: {}", key, e);
            return defaultValue;
        }
    }

    private boolean getBooleanConfigValue(String key, boolean defaultValue) {
        try {
            JsonNode config = getConfiguration();
            JsonNode value = config.at("/" + key.replace(".", "/"));
            return value.isMissingNode() ? defaultValue : value.asBoolean(defaultValue);
        } catch (Exception e) {
            log.warn("Failed to get boolean config value for key: {}", key, e);
            return defaultValue;
        }
    }

    private double getDoubleConfigValue(String key, double defaultValue) {
        try {
            JsonNode config = getConfiguration();
            JsonNode value = config.at("/" + key.replace(".", "/"));
            return value.isMissingNode() ? defaultValue : value.asDouble(defaultValue);
        } catch (Exception e) {
            log.warn("Failed to get double config value for key: {}", key, e);
            return defaultValue;
        }
    }

    /**
     * Feature flag constants
     */
    public static class Features {
        public static final String AI_IMAGE_GENERATION = "ai.image.generation.enabled";
        public static final String PUSH_NOTIFICATIONS = "push.notifications.enabled";
        public static final String VIDEO_CALLS = "video.calls.enabled";
        public static final String EXTERNAL_RIDES = "external.rides.enabled";
        public static final String SOCIAL_FEED = "social.feed.enabled";
        public static final String REVIEWS_SYSTEM = "reviews.system.enabled";
        public static final String BOOKING_SYSTEM = "booking.system.enabled";
        public static final String PAYMENT_PROCESSING = "payment.processing.enabled";
        public static final String EMAIL_NOTIFICATIONS = "email.notifications.enabled";
        public static final String SMS_NOTIFICATIONS = "sms.notifications.enabled";
    }

    /**
     * Service configuration constants
     */
    public static class Services {
        public static final String AUTH = "auth";
        public static final String IMAGE = "image";
        public static final String SOCIAL = "social";
        public static final String BOOKING = "booking";
        public static final String RIDE = "ride";
        public static final String SHOPPING = "shopping";
        public static final String COMMUNICATION = "communication";
        public static final String REVIEWS = "reviews";
    }
}
