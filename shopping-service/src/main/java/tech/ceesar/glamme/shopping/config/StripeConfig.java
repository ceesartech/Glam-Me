package tech.ceesar.glamme.shopping.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@RequiredArgsConstructor
public class StripeConfig {

    private final SecretsManagerClient secretsManagerClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${aws.secrets.stripe-arn:}")
    private String stripeSecretArn;

    @PostConstruct
    public void init() {
        try {
            String apiKey = getStripeApiKeyFromSecrets();
            Stripe.apiKey = apiKey;
        } catch (Exception e) {
            // Fallback to environment variable if Secrets Manager fails
            Stripe.apiKey = System.getenv("STRIPE_API_KEY");
        }
    }

    private String getStripeApiKeyFromSecrets() {
        if (stripeSecretArn == null || stripeSecretArn.isEmpty()) {
            throw new RuntimeException("Stripe secret ARN not configured");
        }

        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(stripeSecretArn)
                    .build();

            var response = secretsManagerClient.getSecretValue(request);
            String secretString = response.secretString();

            JsonNode secretJson = objectMapper.readTree(secretString);
            return secretJson.get("api_key").asText();

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve Stripe API key from Secrets Manager", e);
        }
    }
}
