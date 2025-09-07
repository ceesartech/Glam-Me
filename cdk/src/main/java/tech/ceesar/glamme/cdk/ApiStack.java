package tech.ceesar.glamme.cdk;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.*;
import static software.amazon.awscdk.services.apigateway.Cors.*;
import software.amazon.awscdk.services.cognito.IUserPool;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.constructs.Construct;

import java.util.Map;

import java.util.Arrays;

public class ApiStack extends Stack {
    private final RestApi restApi;

    public ApiStack(final Construct scope, final String id, final StackProps props,
                    final IVpc vpc, final NetworkLoadBalancer nlb, final IUserPool userPool) {
        super(scope, id, props);

        // REST API Gateway with Cognito Authorizer
        CognitoUserPoolsAuthorizer cognitoAuthorizer = CognitoUserPoolsAuthorizer.Builder.create(this, "CognitoAuthorizer")
                .cognitoUserPools(Arrays.asList(userPool))
                .build();

        this.restApi = RestApi.Builder.create(this, "GlammeRestApi")
                .restApiName("glamme-api")
                .description("GlamMe API Gateway")
                .defaultCorsPreflightOptions(CorsOptions.builder()
                        .allowOrigins(Arrays.asList("*"))
                        .allowMethods(Cors.ALL_METHODS)
                        .allowHeaders(Arrays.asList("Content-Type", "Authorization", "X-Amz-Date", "X-Api-Key"))
                        .build())
                .build();

        // Create API resources and methods for each service
        createApiResources(nlb);

        // Outputs
        software.amazon.awscdk.CfnOutput.Builder.create(this, "ApiEndpoint")
                .exportName("ApiEndpoint")
                .value(restApi.getUrl())
                .build();
    }

    private void createApiResources(NetworkLoadBalancer nlb) {
        // Simple health check endpoint
        IResource healthResource = restApi.getRoot().addResource("health");
        healthResource.addMethod("GET", MockIntegration.Builder.create().build());
    }

    public RestApi getRestApi() {
        return restApi;
    }
}
