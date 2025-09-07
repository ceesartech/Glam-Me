package tech.ceesar.glamme.cdk;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.cloudwatch.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.logs.*;
import software.amazon.awscdk.services.synthetics.*;
import software.amazon.awscdk.services.xray.*;
import software.amazon.awscdk.services.events.*;
import software.amazon.awscdk.services.events.targets.*;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ObservabilityStack extends Stack {
    private final Dashboard dashboard;

    public ObservabilityStack(final Construct scope, final String id, final StackProps props,
                              final List<ApplicationLoadBalancedFargateService> services) {
        super(scope, id, props);

        // Basic CloudWatch setup (X-Ray removed for compatibility)

        // Comprehensive CloudWatch Dashboard
        this.dashboard = createComprehensiveDashboard(services);

        // CloudWatch Alarms
        setupCloudWatchAlarms();

        // Basic CloudWatch setup (advanced features removed for compatibility)

        // Log Insights and Metric Filters
        setupLogInsightsAndFilters();

        // Outputs
        software.amazon.awscdk.CfnOutput.Builder.create(this, "DashboardUrl")
                .exportName("DashboardUrl")
                .value("https://console.aws.amazon.com/cloudwatch/home?region=" +
                        Stack.of(this).getRegion() + "#dashboards:name=" + dashboard.getDashboardName())
                .build();

    }


    private Dashboard createComprehensiveDashboard(List<ApplicationLoadBalancedFargateService> services) {
        return Dashboard.Builder.create(this, "GlammeDashboard")
                .dashboardName("Glamme-Observability")
                .widgets(Arrays.asList(Arrays.asList(
                        // Row 1: Service Health Overview
                        GraphWidget.Builder.create()
                                .title("Service Health Overview")
                                .left(Arrays.asList(
                                        createServiceMetric("auth-service", "CPUUtilization"),
                                        createServiceMetric("image-service", "CPUUtilization"),
                                        createServiceMetric("shopping-service", "CPUUtilization"),
                                        createServiceMetric("ride-service", "CPUUtilization")))
                                .right(Arrays.asList(
                                        createServiceMetric("auth-service", "MemoryUtilization"),
                                        createServiceMetric("image-service", "MemoryUtilization"),
                                        createServiceMetric("shopping-service", "MemoryUtilization"),
                                        createServiceMetric("ride-service", "MemoryUtilization")))
                                .width(24)
                                .height(6)
                                .build(),

                        // Row 2: API Gateway Performance
                        GraphWidget.Builder.create()
                                .title("API Gateway Performance")
                                .left(Arrays.asList(
                                        Metric.Builder.create()
                                                .namespace("AWS/ApiGateway")
                                                .metricName("Count")
                                                .dimensionsMap(Map.of("ApiName", "glamme-api"))
                                                .statistic("Sum")
                                                .build(),
                                        Metric.Builder.create()
                                                .namespace("AWS/ApiGateway")
                                                .metricName("Latency")
                                                .dimensionsMap(Map.of("ApiName", "glamme-api"))
                                                .statistic("Average")
                                                .build()))
                                .right(Arrays.asList(
                                        Metric.Builder.create()
                                                .namespace("AWS/ApiGateway")
                                                .metricName("4XXError")
                                                .dimensionsMap(Map.of("ApiName", "glamme-api"))
                                                .statistic("Sum")
                                                .build(),
                                        Metric.Builder.create()
                                                .namespace("AWS/ApiGateway")
                                                .metricName("5XXError")
                                                .dimensionsMap(Map.of("ApiName", "glamme-api"))
                                                .statistic("Sum")
                                                .build()))
                                .width(24)
                                .height(6)
                                .build(),

                        // Row 3: Database Performance
                        GraphWidget.Builder.create()
                                .title("Database Performance")
                                .left(Arrays.asList(
                                        Metric.Builder.create()
                                                .namespace("AWS/RDS")
                                                .metricName("DatabaseConnections")
                                                .dimensionsMap(Map.of("DBClusterIdentifier", "glamme-database"))
                                                .build(),
                                        Metric.Builder.create()
                                                .namespace("AWS/RDS")
                                                .metricName("CPUUtilization")
                                                .dimensionsMap(Map.of("DBClusterIdentifier", "glamme-database"))
                                                .build()))
                                .right(Arrays.asList(
                                        Metric.Builder.create()
                                                .namespace("AWS/RDS")
                                                .metricName("FreeStorageSpace")
                                                .dimensionsMap(Map.of("DBClusterIdentifier", "glamme-database"))
                                                .build(),
                                        Metric.Builder.create()
                                                .namespace("AWS/RDS")
                                                .metricName("ReadLatency")
                                                .dimensionsMap(Map.of("DBClusterIdentifier", "glamme-database"))
                                                .build()))
                                .width(24)
                                .height(6)
                                .build(),

                        // Row 4: Queue and Event Processing
                        GraphWidget.Builder.create()
                                .title("Queue & Event Processing")
                                .left(Arrays.asList(
                                        Metric.Builder.create()
                                                .namespace("AWS/SQS")
                                                .metricName("ApproximateNumberOfMessagesVisible")
                                                .dimensionsMap(Map.of("QueueName", "image-jobs"))
                                                .build(),
                                        Metric.Builder.create()
                                                .namespace("AWS/SQS")
                                                .metricName("ApproximateNumberOfMessagesVisible")
                                                .dimensionsMap(Map.of("QueueName", "booking-events"))
                                                .build()))
                                .right(Arrays.asList(
                                        Metric.Builder.create()
                                                .namespace("AWS/Events")
                                                .metricName("InvocationsCount")
                                                .dimensionsMap(Map.of("RuleName", "glamme-events"))
                                                .statistic("Sum")
                                                .build(),
                                        Metric.Builder.create()
                                                .namespace("AWS/Events")
                                                .metricName("FailedInvocations")
                                                .dimensionsMap(Map.of("RuleName", "glamme-events"))
                                                .statistic("Sum")
                                                .build()))
                                .width(24)
                                .height(6)
                                .build(),

                        // Row 5: Business Metrics (Custom Application Metrics)
                        GraphWidget.Builder.create()
                                .title("Business Metrics")
                                .left(Arrays.asList(
                                        Metric.Builder.create()
                                                .namespace("GlamMe/Application")
                                                .metricName("UserRegistrations")
                                                .statistic("Sum")
                                                .build(),
                                        Metric.Builder.create()
                                                .namespace("GlamMe/Application")
                                                .metricName("RideRequests")
                                                .statistic("Sum")
                                                .build()))
                                .right(Arrays.asList(
                                        Metric.Builder.create()
                                                .namespace("GlamMe/Application")
                                                .metricName("PaymentProcessed")
                                                .statistic("Sum")
                                                .build(),
                                        Metric.Builder.create()
                                                .namespace("GlamMe/Application")
                                                .metricName("ImageProcessed")
                                                .statistic("Sum")
                                                .build()))
                                .width(24)
                                .height(6)
                                .build(),

                        // Row 6: Error Tracking
                        GraphWidget.Builder.create()
                                .title("Error Tracking")
                                .left(Arrays.asList(
                                        Metric.Builder.create()
                                                .namespace("GlamMe/Application")
                                                .metricName("ApplicationErrors")
                                                .dimensionsMap(Map.of("ServiceName", "auth-service"))
                                                .build(),
                                        Metric.Builder.create()
                                                .namespace("GlamMe/Application")
                                                .metricName("ApplicationErrors")
                                                .dimensionsMap(Map.of("ServiceName", "image-service"))
                                                .build()))
                                .right(Arrays.asList(
                                        Metric.Builder.create()
                                                .namespace("GlamMe/Application")
                                                .metricName("ApplicationErrors")
                                                .dimensionsMap(Map.of("ServiceName", "shopping-service"))
                                                .build(),
                                        Metric.Builder.create()
                                                .namespace("GlamMe/Application")
                                                .metricName("ApplicationErrors")
                                                .dimensionsMap(Map.of("ServiceName", "ride-service"))
                                                .build()))
                                .width(24)
                                .height(6)
                                .build())))
                .build();
    }

    private Metric createServiceMetric(String serviceName, String metricName) {
        return Metric.Builder.create()
                .namespace("AWS/ECS")
                .metricName(metricName)
                .dimensionsMap(Map.of("ServiceName", serviceName))
                .build();
    }

    private void setupCloudWatchAlarms() {
        // Service Health Alarms
        createServiceAlarm("auth-service", "CPUUtilization", 80, "CPU");
        createServiceAlarm("image-service", "CPUUtilization", 80, "CPU");
        createServiceAlarm("shopping-service", "CPUUtilization", 80, "CPU");
        createServiceAlarm("ride-service", "CPUUtilization", 80, "CPU");

        // API Gateway Alarms
        Alarm.Builder.create(this, "ApiGatewayHighErrorRate")
                .alarmName("Glamme-API-High-5XX-Error-Rate")
                .alarmDescription("API Gateway 5XX error rate is too high")
                .metric(Metric.Builder.create()
                        .namespace("AWS/ApiGateway")
                        .metricName("5XXError")
                        .dimensionsMap(Map.of("ApiName", "glamme-api"))
                        .build())
                .threshold(5)
                .evaluationPeriods(3)
                .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
                .build();

        // Database Alarms
        Alarm.Builder.create(this, "DatabaseHighConnections")
                .alarmName("Glamme-DB-High-Connections")
                .alarmDescription("Database has too many connections")
                .metric(Metric.Builder.create()
                        .namespace("AWS/RDS")
                        .metricName("DatabaseConnections")
                        .dimensionsMap(Map.of("DBClusterIdentifier", "glamme-database"))
                        .build())
                .threshold(80)
                .evaluationPeriods(2)
                .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
                .build();

        // Queue Alarms
        Alarm.Builder.create(this, "QueueDepthAlarm")
                .alarmName("Glamme-Queue-High-Depth")
                .alarmDescription("Queue has too many messages")
                .metric(Metric.Builder.create()
                        .namespace("AWS/SQS")
                        .metricName("ApproximateNumberOfMessagesVisible")
                        .dimensionsMap(Map.of("QueueName", "image-jobs"))
                        .build())
                .threshold(100)
                .evaluationPeriods(2)
                .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
                .build();

        // Business Metric Alarms
        Alarm.Builder.create(this, "LowUserRegistration")
                .alarmName("Glamme-Low-User-Registrations")
                .alarmDescription("User registration rate is too low")
                .metric(Metric.Builder.create()
                        .namespace("GlamMe/Application")
                        .metricName("UserRegistrations")
                        .statistic("Sum")
                        .build())
                .threshold(5)
                .evaluationPeriods(24) // 24 hours
                .comparisonOperator(ComparisonOperator.LESS_THAN_THRESHOLD)
                .build();
    }

    private void createServiceAlarm(String serviceName, String metricName, double threshold, String alarmType) {
        Alarm.Builder.create(this, serviceName + "-" + alarmType + "-Alarm")
                .alarmName("Glamme-" + serviceName + "-High-" + alarmType)
                .alarmDescription(alarmType + " utilization is too high for " + serviceName)
                .metric(Metric.Builder.create()
                        .namespace("AWS/ECS")
                        .metricName(metricName)
                        .dimensionsMap(Map.of("ServiceName", serviceName))
                        .build())
                .threshold(threshold)
                .evaluationPeriods(3)
                .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
                .build();
    }


    private void setupLogInsightsAndFilters() {
        // Log Groups for application logs
        LogGroup applicationLogGroup = LogGroup.Builder.create(this, "GlammeApplicationLogs")
                .logGroupName("/aws/glamme/application")
                .retention(RetentionDays.ONE_MONTH)
                .build();

        // Metric Filter for Error Logs
        MetricFilter.Builder.create(this, "ErrorMetricFilter")
                .logGroup(applicationLogGroup)
                .metricNamespace("GlamMe/Application")
                .metricName("ApplicationErrors")
                .filterPattern(software.amazon.awscdk.services.logs.FilterPattern.literal("?ERROR ?WARN ?FATAL ?CRITICAL"))
                .metricValue("1")
                .build();

        // Metric Filter for User Registrations
        MetricFilter.Builder.create(this, "UserRegistrationMetricFilter")
                .logGroup(applicationLogGroup)
                .metricNamespace("GlamMe/Application")
                .metricName("UserRegistrations")
                .filterPattern(software.amazon.awscdk.services.logs.FilterPattern.literal("{ $.eventType = \"user.registered\" }"))
                .metricValue("1")
                .build();

        // Metric Filter for Ride Requests
        MetricFilter.Builder.create(this, "RideRequestMetricFilter")
                .logGroup(applicationLogGroup)
                .metricNamespace("GlamMe/Application")
                .metricName("RideRequests")
                .filterPattern(software.amazon.awscdk.services.logs.FilterPattern.literal("{ $.eventType = \"ride.requested\" }"))
                .metricValue("1")
                .build();

        // Metric Filter for Payments
        MetricFilter.Builder.create(this, "PaymentMetricFilter")
                .logGroup(applicationLogGroup)
                .metricNamespace("GlamMe/Application")
                .metricName("PaymentProcessed")
                .filterPattern(software.amazon.awscdk.services.logs.FilterPattern.literal("{ $.eventType = \"payment.succeeded\" }"))
                .metricValue("1")
                .build();

        // Metric Filter for Image Processing
        MetricFilter.Builder.create(this, "ImageProcessingMetricFilter")
                .logGroup(applicationLogGroup)
                .metricNamespace("GlamMe/Application")
                .metricName("ImageProcessed")
                .filterPattern(software.amazon.awscdk.services.logs.FilterPattern.literal("{ $.eventType = \"image.job.completed\" }"))
                .metricValue("1")
                .build();
    }

    public Dashboard getDashboard() {
        return dashboard;
    }
}
