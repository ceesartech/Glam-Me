package tech.ceesar.glamme.shopping.config;

import com.easypost.EasyPost;
import com.easypost.exception.General.MissingParameterError;
import com.easypost.service.EasyPostClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

@Configuration
@RequiredArgsConstructor
public class EasyPostConfig {

    private final SecretsManagerClient secretsManagerClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${aws.secrets.easypost-arn:}")
    private String easyPostSecretArn;

    @Bean
    public EasyPostClient easyPostClient() throws MissingParameterError {
        try {
            String apiKey = getEasyPostApiKeyFromSecrets();
            return new EasyPostClient(apiKey);
        } catch (Exception e) {
            // Fallback to environment variable if Secrets Manager fails
            String fallbackApiKey = System.getenv("EASYPOST_API_KEY");
            if (fallbackApiKey != null) {
                return new EasyPostClient(fallbackApiKey);
            }
            throw new RuntimeException("EasyPost API key not configured");
        }
    }

    private String getEasyPostApiKeyFromSecrets() {
        if (easyPostSecretArn == null || easyPostSecretArn.isEmpty()) {
            throw new RuntimeException("EasyPost secret ARN not configured");
        }

        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(easyPostSecretArn)
                    .build();

            var response = secretsManagerClient.getSecretValue(request);
            String secretString = response.secretString();

            JsonNode secretJson = objectMapper.readTree(secretString);
            return secretJson.get("api_key").asText();

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve EasyPost API key from Secrets Manager", e);
        }
    }
}
