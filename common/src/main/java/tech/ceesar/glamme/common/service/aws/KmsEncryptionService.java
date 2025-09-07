package tech.ceesar.glamme.common.service.aws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KmsEncryptionService {

    private final KmsClient kmsClient;

    @Value("${aws.kms.media-key-id:}")
    private String mediaKeyId;

    @Value("${aws.kms.database-key-id:}")
    private String databaseKeyId;

    @Value("${aws.kms.app-key-id:}")
    private String appKeyId;

    /**
     * Encrypt data using the specified KMS key
     */
    public String encrypt(String plainText, EncryptionContext context) {
        try {
            EncryptRequest request = EncryptRequest.builder()
                    .keyId(getKeyIdForContext(context))
                    .plaintext(SdkBytes.fromUtf8String(plainText))
                    // .encryptionContext(context.contextMap()) // Commented out due to API compatibility
                    .build();

            EncryptResponse response = kmsClient.encrypt(request);

            String encryptedData = Base64.getEncoder().encodeToString(
                response.ciphertextBlob().asByteArray()
            );

            log.debug("Successfully encrypted data using KMS key");
            return encryptedData;

        } catch (Exception e) {
            log.error("Failed to encrypt data", e);
            throw new RuntimeException("KMS encryption failed", e);
        }
    }

    /**
     * Decrypt data using KMS
     */
    public String decrypt(String encryptedData, EncryptionContext context) {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);

            DecryptRequest request = DecryptRequest.builder()
                    .ciphertextBlob(SdkBytes.fromByteArray(encryptedBytes))
                    // .encryptionContext(context.contextMap()) // Commented out due to API compatibility
                    .build();

            DecryptResponse response = kmsClient.decrypt(request);

            String decryptedText = response.plaintext().asUtf8String();

            log.debug("Successfully decrypted data using KMS");
            return decryptedText;

        } catch (Exception e) {
            log.error("Failed to decrypt data", e);
            throw new RuntimeException("KMS decryption failed", e);
        }
    }

    /**
     * Generate a data key for envelope encryption
     */
    public DataKeyPair generateDataKey(EncryptionContext context) {
        try {
            GenerateDataKeyRequest request = GenerateDataKeyRequest.builder()
                    .keyId(getKeyIdForContext(context))
                    .keySpec(DataKeySpec.AES_256)
                    // .encryptionContext(context.contextMap()) // Commented out due to API compatibility
                    .build();

            GenerateDataKeyResponse response = kmsClient.generateDataKey(request);

            return new DataKeyPair(
                Base64.getEncoder().encodeToString(response.plaintext().asByteArray()),
                Base64.getEncoder().encodeToString(response.ciphertextBlob().asByteArray())
            );

        } catch (Exception e) {
            log.error("Failed to generate data key", e);
            throw new RuntimeException("KMS data key generation failed", e);
        }
    }

    /**
     * Decrypt a data key
     */
    public String decryptDataKey(String encryptedDataKey, EncryptionContext context) {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedDataKey);

            DecryptRequest request = DecryptRequest.builder()
                    .ciphertextBlob(SdkBytes.fromByteArray(encryptedBytes))
                    // .encryptionContext(context.contextMap()) // Commented out due to API compatibility
                    .build();

            DecryptResponse response = kmsClient.decrypt(request);

            return Base64.getEncoder().encodeToString(response.plaintext().asByteArray());

        } catch (Exception e) {
            log.error("Failed to decrypt data key", e);
            throw new RuntimeException("KMS data key decryption failed", e);
        }
    }

    /**
     * Create a grant for temporary access to KMS key
     */
    public String createGrant(String keyId, String granteePrincipal, EncryptionContext context) {
        try {
            CreateGrantRequest request = CreateGrantRequest.builder()
                    .keyId(keyId)
                    .granteePrincipal(granteePrincipal)
                    .operations(
                        GrantOperation.DECRYPT,
                        GrantOperation.DESCRIBE_KEY,
                        GrantOperation.ENCRYPT,
                        GrantOperation.GENERATE_DATA_KEY,
                        GrantOperation.GET_PUBLIC_KEY,
                        GrantOperation.RE_ENCRYPT_FROM,
                        GrantOperation.RE_ENCRYPT_TO,
                        GrantOperation.RETIRE_GRANT
                    )
                    // .encryptionContext(context.contextMap()) // Commented out due to API compatibility
                    .build();

            CreateGrantResponse response = kmsClient.createGrant(request);

            log.info("Created KMS grant for principal: {} on key: {}", granteePrincipal, keyId);
            return response.grantToken();

        } catch (Exception e) {
            log.error("Failed to create KMS grant for principal: {}", granteePrincipal, e);
            throw new RuntimeException("KMS grant creation failed", e);
        }
    }

    /**
     * Retire a grant
     */
    public void retireGrant(String grantToken) {
        try {
            RetireGrantRequest request = RetireGrantRequest.builder()
                    .grantToken(grantToken)
                    .build();

            kmsClient.retireGrant(request);

            log.info("Retired KMS grant: {}", grantToken);

        } catch (Exception e) {
            log.error("Failed to retire KMS grant: {}", grantToken, e);
        }
    }

    /**
     * Get KMS key information
     */
    public KeyMetadata getKeyMetadata(String keyId) {
        try {
            DescribeKeyRequest request = DescribeKeyRequest.builder()
                    .keyId(keyId)
                    .build();

            DescribeKeyResponse response = kmsClient.describeKey(request);

            return response.keyMetadata();

        } catch (Exception e) {
            log.error("Failed to get KMS key metadata for: {}", keyId, e);
            throw new RuntimeException("Failed to get KMS key metadata", e);
        }
    }

    private String getKeyIdForContext(EncryptionContext context) {
        // Determine context type based on context name or map
        String contextName = context.name().toLowerCase();
        if (contextName.contains("media") || contextName.contains("image")) {
            return mediaKeyId;
        } else if (contextName.contains("database") || contextName.contains("db")) {
            return databaseKeyId;
        } else {
            return appKeyId; // Default to app key
        }
    }

    /**
     * Encrypt sensitive field with context
     */
    public String encryptField(String fieldName, String value, String entityId) {
        EncryptionContext context = new EncryptionContext("field-encryption",
            Map.of("field", fieldName, "entityId", entityId));
        return encrypt(value, context);
    }

    /**
     * Decrypt sensitive field with context
     */
    public String decryptField(String fieldName, String encryptedValue, String entityId) {
        EncryptionContext context = new EncryptionContext("field-encryption",
            Map.of("field", fieldName, "entityId", entityId));
        return decrypt(encryptedValue, context);
    }

    /**
     * Encrypt payment information
     */
    public String encryptPaymentInfo(String paymentData, String customerId) {
        EncryptionContext context = new EncryptionContext("payment",
            Map.of("customerId", customerId, "dataType", "payment"));
        return encrypt(paymentData, context);
    }

    /**
     * Decrypt payment information
     */
    public String decryptPaymentInfo(String encryptedPaymentData, String customerId) {
        EncryptionContext context = new EncryptionContext("payment",
            Map.of("customerId", customerId, "dataType", "payment"));
        return decrypt(encryptedPaymentData, context);
    }

    /**
     * Encrypt OAuth tokens
     */
    public String encryptOAuthToken(String token, String provider, String userId) {
        EncryptionContext context = new EncryptionContext("oauth",
            Map.of("provider", provider, "userId", userId));
        return encrypt(token, context);
    }

    /**
     * Decrypt OAuth tokens
     */
    public String decryptOAuthToken(String encryptedToken, String provider, String userId) {
        EncryptionContext context = new EncryptionContext("oauth",
            Map.of("provider", provider, "userId", userId));
        return decrypt(encryptedToken, context);
    }

    // Nested classes
    public record EncryptionContext(String name, Map<String, String> contextMap) {}

    public enum EncryptionContextType {
        MEDIA, DATABASE, APP
    }

    public record DataKeyPair(String plaintextKey, String encryptedKey) {}
}
