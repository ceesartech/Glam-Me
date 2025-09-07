package tech.ceesar.glamme.cdk;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.cloudfront.*;
import software.amazon.awscdk.services.cloudfront.origins.S3Origin;
import software.amazon.awscdk.services.s3.Bucket;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.Map;

public class EdgeStack extends Stack {
    private final Distribution distribution;

    public EdgeStack(final Construct scope, final String id, final StackProps props,
                     final Bucket uiBucket, final RestApi restApi, final Bucket mediaBucket) {
        super(scope, id, props);

        // Simple CloudFront Distribution for UI bucket only
        this.distribution = Distribution.Builder.create(this, "GlammeDistribution")
                .defaultBehavior(BehaviorOptions.builder()
                        .origin(new S3Origin(uiBucket))
                        .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                        .cachePolicy(CachePolicy.CACHING_OPTIMIZED)
                        .build())
                .defaultRootObject("index.html")
                .errorResponses(Arrays.asList(
                        ErrorResponse.builder()
                                .httpStatus(404)
                                .responseHttpStatus(200)
                                .responsePagePath("/index.html")
                                .build()))
                .build();

        // Outputs
        software.amazon.awscdk.CfnOutput.Builder.create(this, "DistributionUrl")
                .exportName("DistributionUrl")
                .value("https://" + distribution.getDistributionDomainName())
                .build();

        software.amazon.awscdk.CfnOutput.Builder.create(this, "DistributionId")
                .exportName("DistributionId")
                .value(distribution.getDistributionId())
                .build();
    }

    public Distribution getDistribution() {
        return distribution;
    }
}
