package tech.ceesar.glamme.shopping.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * AWS-based shipping configuration replacing EasyPost
 * Uses SES for email notifications and S3 for shipping document storage
 */
@Configuration
@RequiredArgsConstructor
public class ShippingConfig {

    @Bean
    public S3Client shippingDocumentClient() {
        return S3Client.builder()
                .build();
    }

    @Bean
    public SesClient shippingNotificationClient() {
        return SesClient.builder()
                .build();
    }
}
