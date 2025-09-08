package tech.ceesar.glamme.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for AWS Cognito integration
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aws.cognito")
public class CognitoConfig {
    
    /**
     * Cognito User Pool ID
     */
    private String userPoolId;
    
    /**
     * Cognito Client ID
     */
    private String clientId;
    
    /**
     * Cognito Client Secret (optional)
     */
    private String clientSecret;
    
    /**
     * AWS Region for Cognito
     */
    private String region = "us-east-1";
    
    /**
     * Cognito Domain for hosted UI (optional)
     */
    private String domain;
    
    /**
     * Redirect URI for OAuth (optional)
     */
    private String redirectUri;
    
    /**
     * Logout URI for OAuth (optional)
     */
    private String logoutUri;
    
    /**
     * JWT Token expiration time in seconds
     */
    private long tokenExpirationSeconds = 3600; // 1 hour
    
    /**
     * Enable/disable Cognito authentication
     */
    private boolean enabled = true;
}