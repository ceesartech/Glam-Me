package tech.ceesar.glamme.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
public class DatabaseConfig {

    @Value("${aws.secrets.database-arn:}")
    private String databaseSecretArn;

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        DataSourceProperties properties = new DataSourceProperties();

        if (databaseSecretArn != null && !databaseSecretArn.isEmpty()) {
            // Retrieve database credentials from AWS Secrets Manager
            try (SecretsManagerClient client = SecretsManagerClient.create()) {
                GetSecretValueRequest request = GetSecretValueRequest.builder()
                        .secretId(databaseSecretArn)
                        .build();

                GetSecretValueResponse response = client.getSecretValue(request);
                String secretString = response.secretString();

                // Parse the JSON secret
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, String> secretMap = mapper.readValue(secretString, Map.class);

                properties.setUrl(String.format("jdbc:postgresql://%s:%s/%s",
                        System.getenv("DB_HOST"),
                        System.getenv("DB_PORT"),
                        System.getenv("DB_NAME")));

                properties.setUsername(secretMap.get("username"));
                properties.setPassword(secretMap.get("password"));

            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve database credentials from Secrets Manager", e);
            }
        }

        return properties;
    }

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource(DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }
}
