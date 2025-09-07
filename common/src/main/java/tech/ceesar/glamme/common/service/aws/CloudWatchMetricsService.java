package tech.ceesar.glamme.common.service.aws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudWatchMetricsService {

    private final CloudWatchClient cloudWatchClient;

    /**
     * Publish a single metric
     */
    public void publishMetric(String namespace, String metricName, double value) {
        publishMetric(namespace, metricName, value, StandardUnit.COUNT, null);
    }

    /**
     * Publish a metric with dimensions
     */
    public void publishMetric(String namespace, String metricName, double value,
                            StandardUnit unit, Map<String, String> dimensions) {
        try {
            MetricDatum.Builder metricBuilder = MetricDatum.builder()
                    .metricName(metricName)
                    .value(value)
                    .unit(unit)
                    .timestamp(Instant.now());

            if (dimensions != null && !dimensions.isEmpty()) {
                metricBuilder.dimensions(dimensions.entrySet().stream()
                        .map(entry -> Dimension.builder()
                                .name(entry.getKey())
                                .value(entry.getValue())
                                .build())
                        .toList());
            }

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace(namespace)
                    .metricData(metricBuilder.build())
                    .build();

            cloudWatchClient.putMetricData(request);

            log.debug("Published metric: {} to namespace: {} with value: {}", metricName, namespace, value);

        } catch (Exception e) {
            log.error("Failed to publish metric: {}", metricName, e);
        }
    }

    /**
     * Publish multiple metrics in batch
     */
    public void publishMetrics(String namespace, Map<String, Double> metrics) {
        publishMetrics(namespace, metrics, StandardUnit.COUNT, null);
    }

    /**
     * Publish multiple metrics with dimensions
     */
    public void publishMetrics(String namespace, Map<String, Double> metrics,
                             StandardUnit unit, Map<String, String> dimensions) {
        try {
            var metricData = metrics.entrySet().stream()
                    .map(entry -> {
                        MetricDatum.Builder metricBuilder = MetricDatum.builder()
                                .metricName(entry.getKey())
                                .value(entry.getValue())
                                .unit(unit)
                                .timestamp(Instant.now());

                        if (dimensions != null && !dimensions.isEmpty()) {
                            metricBuilder.dimensions(dimensions.entrySet().stream()
                                    .map(dim -> Dimension.builder()
                                            .name(dim.getKey())
                                            .value(dim.getValue())
                                            .build())
                                    .toList());
                        }

                        return metricBuilder.build();
                    })
                    .toList();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace(namespace)
                    .metricData(metricData)
                    .build();

            cloudWatchClient.putMetricData(request);

            log.debug("Published {} metrics to namespace: {}", metrics.size(), namespace);

        } catch (Exception e) {
            log.error("Failed to publish batch metrics", e);
        }
    }

    /**
     * Increment a counter metric
     */
    public void incrementCounter(String namespace, String metricName) {
        publishMetric(namespace, metricName, 1.0, StandardUnit.COUNT, null);
    }

    /**
     * Increment a counter with dimensions
     */
    public void incrementCounter(String namespace, String metricName, Map<String, String> dimensions) {
        publishMetric(namespace, metricName, 1.0, StandardUnit.COUNT, dimensions);
    }

    /**
     * Record response time
     */
    public void recordResponseTime(String namespace, String operation, double milliseconds) {
        publishMetric(namespace, operation + "ResponseTime", milliseconds,
                     StandardUnit.MILLISECONDS, null);
    }

    /**
     * Record response time with dimensions
     */
    public void recordResponseTime(String namespace, String operation, double milliseconds,
                                 Map<String, String> dimensions) {
        publishMetric(namespace, operation + "ResponseTime", milliseconds,
                     StandardUnit.MILLISECONDS, dimensions);
    }

    /**
     * Record error count
     */
    public void recordError(String namespace, String errorType) {
        publishMetric(namespace, errorType + "Errors", 1.0, StandardUnit.COUNT, null);
    }

    /**
     * Record error count with dimensions
     */
    public void recordError(String namespace, String errorType, Map<String, String> dimensions) {
        publishMetric(namespace, errorType + "Errors", 1.0, StandardUnit.COUNT, dimensions);
    }

    /**
     * Business metrics methods
     */
    public void recordUserRegistration(String source) {
        publishMetric("GlamMe/Business", "UserRegistrations", 1.0, StandardUnit.COUNT,
                     Map.of("Source", source));
    }

    public void recordRideRequest(String provider) {
        publishMetric("GlamMe/Business", "RideRequests", 1.0, StandardUnit.COUNT,
                     Map.of("Provider", provider));
    }

    public void recordPaymentProcessed(double amount, String currency) {
        publishMetric("GlamMe/Business", "PaymentAmount", amount, StandardUnit.NONE,
                     Map.of("Currency", currency));
        publishMetric("GlamMe/Business", "PaymentsProcessed", 1.0, StandardUnit.COUNT,
                     Map.of("Currency", currency));
    }

    public void recordImageProcessed(String operation) {
        publishMetric("GlamMe/Business", "ImagesProcessed", 1.0, StandardUnit.COUNT,
                     Map.of("Operation", operation));
    }

    public void recordBookingCreated(String serviceType) {
        publishMetric("GlamMe/Business", "BookingsCreated", 1.0, StandardUnit.COUNT,
                     Map.of("ServiceType", serviceType));
    }

    public void recordApiCall(String endpoint, String method, double responseTime) {
        publishMetric("GlamMe/API", endpoint.replace("/", "_") + "_" + method + "_ResponseTime",
                     responseTime, StandardUnit.MILLISECONDS, null);
        publishMetric("GlamMe/API", "ApiCalls", 1.0, StandardUnit.COUNT,
                     Map.of("Endpoint", endpoint, "Method", method));
    }

    public void recordDatabaseQuery(String operation, double queryTime) {
        publishMetric("GlamMe/Database", operation + "QueryTime", queryTime,
                     StandardUnit.MILLISECONDS, null);
    }

    public void recordCacheHit(String cacheName) {
        publishMetric("GlamMe/Cache", "CacheHits", 1.0, StandardUnit.COUNT,
                     Map.of("CacheName", cacheName));
    }

    public void recordCacheMiss(String cacheName) {
        publishMetric("GlamMe/Cache", "CacheMisses", 1.0, StandardUnit.COUNT,
                     Map.of("CacheName", cacheName));
    }

    /**
     * Performance metrics
     */
    public void recordMemoryUsage(String serviceName, double percentage) {
        publishMetric("GlamMe/Performance", "MemoryUsage", percentage, StandardUnit.PERCENT,
                     Map.of("Service", serviceName));
    }

    public void recordCpuUsage(String serviceName, double percentage) {
        publishMetric("GlamMe/Performance", "CpuUsage", percentage, StandardUnit.PERCENT,
                     Map.of("Service", serviceName));
    }

    public void recordActiveConnections(String serviceName, int count) {
        publishMetric("GlamMe/Performance", "ActiveConnections", count, StandardUnit.COUNT,
                     Map.of("Service", serviceName));
    }

    public void recordQueueDepth(String queueName, int depth) {
        publishMetric("GlamMe/Queue", "QueueDepth", depth, StandardUnit.COUNT,
                     Map.of("QueueName", queueName));
    }

    public void recordEventProcessed(String eventType, double processingTime) {
        publishMetric("GlamMe/Events", eventType + "ProcessingTime", processingTime,
                     StandardUnit.MILLISECONDS, null);
        publishMetric("GlamMe/Events", "EventsProcessed", 1.0, StandardUnit.COUNT,
                     Map.of("EventType", eventType));
    }

    /**
     * Health check metrics
     */
    public void recordHealthCheck(String serviceName, boolean healthy) {
        publishMetric("GlamMe/Health", "HealthStatus", healthy ? 1.0 : 0.0, StandardUnit.COUNT,
                     Map.of("Service", serviceName));
    }

    public void recordDependencyHealth(String dependencyName, boolean healthy) {
        publishMetric("GlamMe/Health", "DependencyHealth", healthy ? 1.0 : 0.0, StandardUnit.COUNT,
                     Map.of("Dependency", dependencyName));
    }
}
