package tech.ceesar.glamme.cdk;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.kms.Key;
import software.amazon.awscdk.services.kms.KeySpec;
import software.amazon.awscdk.services.kms.KeyUsage;
import software.constructs.Construct;

public class SecurityStack extends Stack {
    private final Key kmsKey;

    public SecurityStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Create KMS key for encryption
        this.kmsKey = Key.Builder.create(this, "GlammeKmsKey")
                .description("KMS key for Glamme application data encryption")
                .keySpec(KeySpec.SYMMETRIC_DEFAULT)
                .keyUsage(KeyUsage.ENCRYPT_DECRYPT)
                .enableKeyRotation(true)
                .build();

        // Add alias
        this.kmsKey.addAlias("alias/glamme-app-key");

        // Output the key ARN
        software.amazon.awscdk.CfnOutput.Builder.create(this, "KmsKeyArn")
                .exportName("KmsKeyArn")
                .value(kmsKey.getKeyArn())
                .build();

        software.amazon.awscdk.CfnOutput.Builder.create(this, "KmsKeyId")
                .exportName("KmsKeyId")
                .value(kmsKey.getKeyId())
                .build();
    }

    public Key getKmsKey() {
        return kmsKey;
    }
}
