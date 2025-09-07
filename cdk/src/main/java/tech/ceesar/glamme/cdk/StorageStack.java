package tech.ceesar.glamme.cdk;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.kms.IKey;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketEncryption;
import software.amazon.awscdk.services.s3.IntelligentTieringConfiguration;
import software.amazon.awscdk.services.s3.LifecycleRule;
import software.constructs.Construct;

import java.util.Arrays;

public class StorageStack extends Stack {
    private final Bucket uiBucket;
    private final Bucket mediaBucket;
    private final Bucket productMediaBucket;
    private final Bucket logsBucket;

    public StorageStack(final Construct scope, final String id, final StackProps props, final IKey kmsKey) {
        super(scope, id, props);

        // UI Bucket - for static web assets
        this.uiBucket = Bucket.Builder.create(this, "GlammeUiBucket")
                .bucketName("glamme-ui-" + System.getenv().getOrDefault("ENVIRONMENT", "dev"))
                .versioned(true)
                .encryption(BucketEncryption.S3_MANAGED)
                .publicReadAccess(false)
                .blockPublicAccess(software.amazon.awscdk.services.s3.BlockPublicAccess.BLOCK_ALL)
                .build();

        // Media Bucket - for user-generated content
        this.mediaBucket = Bucket.Builder.create(this, "GlammeMediaBucket")
                .bucketName("glamme-media-" + System.getenv().getOrDefault("ENVIRONMENT", "dev"))
                .versioned(true)
                .encryption(BucketEncryption.KMS)
                .encryptionKey(kmsKey)
                .publicReadAccess(false)
                .blockPublicAccess(software.amazon.awscdk.services.s3.BlockPublicAccess.BLOCK_ALL)
                .intelligentTieringConfigurations(Arrays.asList(
                        IntelligentTieringConfiguration.builder()
                                .prefix("uploads/")
                                .name("UploadsTiering")
                                .build(),
                        IntelligentTieringConfiguration.builder()
                                .prefix("outputs/")
                                .name("OutputsTiering")
                                .build()))
                .lifecycleRules(Arrays.asList(
                        LifecycleRule.builder()
                                .prefix("temp/")
                                .expiration(software.amazon.awscdk.Duration.days(7))
                                .build()))
                .build();

        // Product Media Bucket
        this.productMediaBucket = Bucket.Builder.create(this, "GlammeProductMediaBucket")
                .bucketName("glamme-product-media-" + System.getenv().getOrDefault("ENVIRONMENT", "dev"))
                .versioned(true)
                .encryption(BucketEncryption.KMS)
                .encryptionKey(kmsKey)
                .publicReadAccess(false)
                .blockPublicAccess(software.amazon.awscdk.services.s3.BlockPublicAccess.BLOCK_ALL)
                .intelligentTieringConfigurations(Arrays.asList(
                        IntelligentTieringConfiguration.builder()
                                .name("ProductImagesTiering")
                                .build()))
                .build();

        // Logs Bucket
        this.logsBucket = Bucket.Builder.create(this, "GlammeLogsBucket")
                .bucketName("glamme-logs-" + System.getenv().getOrDefault("ENVIRONMENT", "dev"))
                .versioned(true)
                .encryption(BucketEncryption.KMS)
                .encryptionKey(kmsKey)
                .publicReadAccess(false)
                .blockPublicAccess(software.amazon.awscdk.services.s3.BlockPublicAccess.BLOCK_ALL)
                .lifecycleRules(Arrays.asList(
                        LifecycleRule.builder()
                                .prefix("logs/")
                                .transitions(Arrays.asList(
                                        software.amazon.awscdk.services.s3.Transition.builder()
                                                .storageClass(software.amazon.awscdk.services.s3.StorageClass.GLACIER)
                                                .transitionAfter(software.amazon.awscdk.Duration.days(90))
                                                .build(),
                                        software.amazon.awscdk.services.s3.Transition.builder()
                                                .storageClass(software.amazon.awscdk.services.s3.StorageClass.DEEP_ARCHIVE)
                                                .transitionAfter(software.amazon.awscdk.Duration.days(365))
                                                .build()))
                                .expiration(software.amazon.awscdk.Duration.days(2555)) // 7 years
                                .build()))
                .build();

        // Outputs
        software.amazon.awscdk.CfnOutput.Builder.create(this, "UiBucketName")
                .exportName("UiBucketName")
                .value(uiBucket.getBucketName())
                .build();

        software.amazon.awscdk.CfnOutput.Builder.create(this, "MediaBucketName")
                .exportName("MediaBucketName")
                .value(mediaBucket.getBucketName())
                .build();

        software.amazon.awscdk.CfnOutput.Builder.create(this, "ProductMediaBucketName")
                .exportName("ProductMediaBucketName")
                .value(productMediaBucket.getBucketName())
                .build();

        software.amazon.awscdk.CfnOutput.Builder.create(this, "LogsBucketName")
                .exportName("LogsBucketName")
                .value(logsBucket.getBucketName())
                .build();
    }

    public Bucket getUiBucket() {
        return uiBucket;
    }

    public Bucket getMediaBucket() {
        return mediaBucket;
    }

    public Bucket getProductMediaBucket() {
        return productMediaBucket;
    }

    public Bucket getLogsBucket() {
        return logsBucket;
    }
}
