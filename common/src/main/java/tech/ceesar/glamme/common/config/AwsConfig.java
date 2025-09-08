package tech.ceesar.glamme.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.opensearch.OpenSearchClient;
// import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient; // Commented out - not available in this SDK version
// import org.opensearch.client.opensearch.OpenSearchClient; // Removed due to naming conflict
// import org.opensearch.client.transport.aws.AwsSdk2Transport; // Removed due to naming conflict
// import org.opensearch.client.transport.aws.AwsSdk2TransportOptions; // Removed due to naming conflict

@Configuration
public class AwsConfig {

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
                .build();
    }

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
                .build();
    }

    @Bean
    public EventBridgeClient eventBridgeClient() {
        return EventBridgeClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
                .build();
    }

    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
                .build();
    }

    @Bean
    public KmsClient kmsClient() {
        return KmsClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
                .build();
    }

    @Bean
    public CloudWatchClient cloudWatchClient() {
        return CloudWatchClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
                .build();
    }

    @Bean
    public CloudWatchLogsClient cloudWatchLogsClient() {
        return CloudWatchLogsClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
                .build();
    }

    @Bean
    public software.amazon.awssdk.services.opensearch.OpenSearchClient openSearchClient() {
        return software.amazon.awssdk.services.opensearch.OpenSearchClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
                .build();
    }

    // @Bean
    // public org.opensearch.client.opensearch.OpenSearchClient openSearchJavaClient() {
    //     // Commented out due to constructor compatibility issues
    //     // Using AWS SDK OpenSearchClient instead
    //     return null;
    // }

    // @Bean
    // public BedrockRuntimeClient bedrockRuntimeClient() {
    //     return BedrockRuntimeClient.builder()
    //             .credentialsProvider(DefaultCredentialsProvider.create())
    //             .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
    //             .build();
    // }

    // @Bean
    // public com.amazonaws.xray.AWSXRayRecorderBuilder awsXRayRecorderBuilder() {
    //     return com.amazonaws.xray.AWSXRayRecorderBuilder.standard()
    //             .withSamplingStrategy(new com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy(
    //                     com.amazonaws.xray.strategy.sampling.AllSamplingStrategy.INSTANCE));
    // }
}
