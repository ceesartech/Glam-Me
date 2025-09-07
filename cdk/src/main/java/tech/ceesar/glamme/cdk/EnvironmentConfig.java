package tech.ceesar.glamme.cdk;

import software.amazon.awscdk.Environment;

public class EnvironmentConfig {
    private final String environment;
    private final String accountId;
    private final String region;
    private final Environment awsEnvironment;

    public EnvironmentConfig() {
        this.accountId = System.getenv().getOrDefault("CDK_DEFAULT_ACCOUNT", "476114151082");
        this.region = System.getenv().getOrDefault("CDK_DEFAULT_REGION", "us-east-1");
        this.environment = System.getenv().getOrDefault("ENVIRONMENT", "dev");

        this.awsEnvironment = Environment.builder()
                .account(accountId)
                .region(region)
                .build();
    }

    public String getStackName(String baseName) {
        return String.format("Glamme%s%sStack", environment.substring(0, 1).toUpperCase() + environment.substring(1), baseName);
    }

    public boolean isProduction() {
        return "prod".equals(environment) || "production".equals(environment);
    }

    public boolean isDevelopment() {
        return "dev".equals(environment) || "development".equals(environment);
    }

    public boolean isStaging() {
        return "staging".equals(environment) || "stage".equals(environment);
    }

    public String getEnvironmentName() {
        return environment;
    }

    public String getResourceName(String baseName) {
        return String.format("glamme-%s-%s", environment, baseName);
    }

    public String getResourceName(String baseName, String suffix) {
        return String.format("glamme-%s-%s-%s", environment, baseName, suffix);
    }

    public Environment getAwsEnvironment() {
        return awsEnvironment;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getRegion() {
        return region;
    }
}
