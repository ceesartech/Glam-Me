package tech.ceesar.glamme.cdk;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocol;
import software.amazon.awscdk.services.rds.DatabaseCluster;
import software.amazon.awscdk.services.rds.AuroraPostgresEngineVersion;
import software.amazon.awscdk.services.rds.ClusterInstance;
import software.amazon.awscdk.services.elasticache.CfnCacheCluster;
import software.amazon.awscdk.services.opensearchservice.Domain;
import software.amazon.awscdk.services.opensearchservice.CapacityConfig;
import software.amazon.awscdk.services.opensearchservice.EngineVersion;
import software.amazon.awscdk.services.opensearchservice.EbsOptions;
import software.amazon.awscdk.services.ecs.EnvironmentFile;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.CorsOptions;
import software.amazon.awscdk.services.apigateway.MethodOptions;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolClient;
import software.amazon.awscdk.services.apigateway.CognitoUserPoolsAuthorizer;
import software.amazon.awscdk.services.cloudfront.Distribution;
import software.amazon.awscdk.services.cloudfront.origins.S3Origin;
import software.amazon.awscdk.services.cloudfront.OriginAccessIdentity;
import software.amazon.awscdk.services.wafv2.CfnWebACL;
import software.amazon.awscdk.services.wafv2.CfnWebACLAssociation;
import software.amazon.awscdk.services.cloudwatch.Dashboard;
import software.amazon.awscdk.services.cloudwatch.GraphWidget;
import software.amazon.awscdk.services.cloudwatch.Alarm;
import software.amazon.awscdk.services.cloudwatch.MathExpression;
import software.amazon.awscdk.services.cloudwatch.Metric;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.CfnOutput;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GlammeCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        new GlammeVpcStack(app, "GlammeVpcStack",
                StackProps.builder()
                        .env(Environment.builder()
                                .account("476114151082")
                                .region("us-east-1")
                                .build())
                        .build());

        app.synth();
    }
}

class GlammeVpcStack extends Stack {
    public GlammeVpcStack(final software.constructs.Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Create VPC with public and private subnets
        Vpc vpc = Vpc.Builder.create(this, "GlammeVpc")
                .maxAzs(2)
                .subnetConfiguration(java.util.Arrays.asList(
                        SubnetConfiguration.builder()
                                .name("Public")
                                .subnetType(software.amazon.awscdk.services.ec2.SubnetType.PUBLIC)
                                .cidrMask(24)
                                .build(),
                        SubnetConfiguration.builder()
                                .name("Private-App")
                                .subnetType(software.amazon.awscdk.services.ec2.SubnetType.PRIVATE_WITH_EGRESS)
                                .cidrMask(24)
                                .build(),
                        SubnetConfiguration.builder()
                                .name("Private-Data")
                                .subnetType(software.amazon.awscdk.services.ec2.SubnetType.PRIVATE_ISOLATED)
                                .cidrMask(24)
                                .build()
                ))
                .natGateways(1)
                .build();

        // Reference existing ECR repositories
        IRepository authRepo = Repository.fromRepositoryName(this, "AuthServiceRepo", "glamme/auth-service");
        IRepository imageRepo = Repository.fromRepositoryName(this, "ImageServiceRepo", "glamme/image-service");
        IRepository matchingRepo = Repository.fromRepositoryName(this, "MatchingServiceRepo", "glamme/matching-service");
        IRepository socialRepo = Repository.fromRepositoryName(this, "SocialServiceRepo", "glamme/social-service");
        IRepository shoppingRepo = Repository.fromRepositoryName(this, "ShoppingServiceRepo", "glamme/shopping-service");
        IRepository communicationRepo = Repository.fromRepositoryName(this, "CommunicationServiceRepo", "glamme/communication-service");

        // Create ECS Cluster
        Cluster cluster = Cluster.Builder.create(this, "GlammeCluster")
                .vpc(vpc)
                .build();

        // Database Security Group
        SecurityGroup dbSecurityGroup = SecurityGroup.Builder.create(this, "DatabaseSecurityGroup")
                .vpc(vpc)
                .description("Security group for database access")
                .build();

        // Create Aurora PostgreSQL Serverless v2
        DatabaseCluster auroraDb = DatabaseCluster.Builder.create(this, "AuroraDatabase")
                .engine(software.amazon.awscdk.services.rds.DatabaseClusterEngine.auroraPostgres(
                        software.amazon.awscdk.services.rds.AuroraPostgresClusterEngineProps.builder()
                                .version(AuroraPostgresEngineVersion.VER_15_4)
                                .build()))
                .credentials(software.amazon.awscdk.services.rds.Credentials.fromGeneratedSecret("glamme_admin"))
                .instanceProps(software.amazon.awscdk.services.rds.InstanceProps.builder()
                        .vpc(vpc)
                        .vpcSubnets(SubnetSelection.builder()
                                .subnetType(software.amazon.awscdk.services.ec2.SubnetType.PRIVATE_WITH_EGRESS)
                                .build())
                        .securityGroups(Arrays.asList(dbSecurityGroup))
                        .build())
                .backup(software.amazon.awscdk.services.rds.BackupProps.builder()
                        .retention(Duration.days(7))
                        .build())
                .build();

        // Create ElastiCache Redis subnet group first
        software.amazon.awscdk.services.elasticache.CfnSubnetGroup redisSubnetGroup = software.amazon.awscdk.services.elasticache.CfnSubnetGroup.Builder.create(this, "RedisSubnetGroup")
                .description("Subnet group for GlamMe Redis cluster")
                .subnetIds(vpc.getPrivateSubnets().stream().map(subnet -> subnet.getSubnetId()).collect(java.util.stream.Collectors.toList()))
                .build();

        // Create ElastiCache Redis
        CfnCacheCluster redisCluster = CfnCacheCluster.Builder.create(this, "RedisCluster")
                .engine("redis")
                .cacheNodeType("cache.t3.micro")
                .engineVersion("7.0")
                .numCacheNodes(1)
                .cacheSubnetGroupName(redisSubnetGroup.getRef())
                .vpcSecurityGroupIds(Arrays.asList(dbSecurityGroup.getSecurityGroupId()))
                .build();

        // Create OpenSearch Serverless Domain
        Domain openSearchDomain = Domain.Builder.create(this, "OpenSearchDomain")
                .version(EngineVersion.OPENSEARCH_2_11)
                .capacity(CapacityConfig.builder()
                        .masterNodes(0)
                        .dataNodes(1)
                        .dataNodeInstanceType("t3.small.search")
                        .build())
                .vpc(vpc)
                .vpcSubnets(Arrays.asList(SubnetSelection.builder()
                        .subnetType(software.amazon.awscdk.services.ec2.SubnetType.PRIVATE_WITH_EGRESS)
                        .build()))
                .securityGroups(Arrays.asList(dbSecurityGroup))
                .ebs(EbsOptions.builder()
                        .volumeSize(20)
                        .volumeType(software.amazon.awscdk.services.ec2.EbsDeviceVolumeType.GP3)
                        .build())
                .build();

        // Create S3 bucket for file storage
        Bucket s3Bucket = Bucket.Builder.create(this, "GlammeBucket")
                .versioned(true)
                .encryption(software.amazon.awscdk.services.s3.BucketEncryption.S3_MANAGED)
                .build();

        // Create Cognito User Pool
        UserPool userPool = UserPool.Builder.create(this, "GlammeUserPool")
                .userPoolName("glamme-user-pool")
                .selfSignUpEnabled(true)
                .passwordPolicy(software.amazon.awscdk.services.cognito.PasswordPolicy.builder()
                        .minLength(8)
                        .requireLowercase(true)
                        .requireUppercase(true)
                        .requireDigits(true)
                        .build())
                .build();

        UserPoolClient userPoolClient = UserPoolClient.Builder.create(this, "GlammeUserPoolClient")
                .userPool(userPool)
                .build();

        // TODO: Create API Gateway with methods when needed
        // RestApi api = RestApi.Builder.create(this, "GlammeApi")
        //         .restApiName("glamme-api")
        //         .description("GlamMe Platform API")
        //         .deployOptions(software.amazon.awscdk.services.apigateway.StageOptions.builder()
        //                 .stageName("prod")
        //                 .build())
        //         .build();

        // TODO: Create Cognito Authorizer for API Gateway when needed
        // CognitoUserPoolsAuthorizer authorizer = CognitoUserPoolsAuthorizer.Builder.create(this, "GlammeAuthorizer")
        //         .cognitoUserPools(Arrays.asList(userPool))
        //         .build();

        // Common environment variables for all services
        Map<String, String> commonEnvVars = Map.of(
                "SPRING_PROFILES_ACTIVE", "prod",
                "AWS_REGION", "us-east-1",
                "DB_HOST", auroraDb.getClusterEndpoint().getHostname(),
                "DB_PORT", auroraDb.getClusterEndpoint().getPort().toString(),
                "REDIS_HOST", redisCluster.getRef(),
                "REDIS_PORT", "6379",
                "OPENSEARCH_ENDPOINT", openSearchDomain.getDomainEndpoint(),
                "S3_BUCKET", s3Bucket.getBucketName(),
                "COGNITO_USER_POOL_ID", userPool.getUserPoolId(),
                "COGNITO_CLIENT_ID", userPoolClient.getUserPoolClientId()
        );

        // Create ECS Services for each microservice
        ApplicationLoadBalancedFargateService authService = createFargateService(this, cluster, vpc, "AuthService", authRepo, commonEnvVars, true);
        ApplicationLoadBalancedFargateService imageService = createFargateService(this, cluster, vpc, "ImageService", imageRepo, commonEnvVars, false);
        ApplicationLoadBalancedFargateService matchingService = createFargateService(this, cluster, vpc, "MatchingService", matchingRepo, commonEnvVars, false);
        ApplicationLoadBalancedFargateService socialService = createFargateService(this, cluster, vpc, "SocialService", socialRepo, commonEnvVars, false);
        ApplicationLoadBalancedFargateService shoppingService = createFargateService(this, cluster, vpc, "ShoppingService", shoppingRepo, commonEnvVars, false);
        ApplicationLoadBalancedFargateService communicationService = createFargateService(this, cluster, vpc, "CommunicationService", communicationRepo, commonEnvVars, false);

        // Add health check paths for each service
        authService.getTargetGroup().setHealthCheck(
            software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck.builder()
                .path("/actuator/health")
                .port("8080")
                .build()
        );

        imageService.getTargetGroup().setHealthCheck(
            software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck.builder()
                .path("/actuator/health")
                .port("8080")
                .build()
        );

        matchingService.getTargetGroup().setHealthCheck(
            software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck.builder()
                .path("/actuator/health")
                .port("8080")
                .build()
        );

        socialService.getTargetGroup().setHealthCheck(
            software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck.builder()
                .path("/actuator/health")
                .port("8080")
                .build()
        );

        shoppingService.getTargetGroup().setHealthCheck(
            software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck.builder()
                .path("/actuator/health")
                .port("8080")
                .build()
        );

        communicationService.getTargetGroup().setHealthCheck(
            software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck.builder()
                .path("/actuator/health")
                .port("8080")
                .build()
        );

        // Create CloudFront Distribution
        OriginAccessIdentity originAccessIdentity = OriginAccessIdentity.Builder.create(this, "OAI")
                .build();

        Distribution distribution = Distribution.Builder.create(this, "GlammeDistribution")
                .defaultBehavior(software.amazon.awscdk.services.cloudfront.BehaviorOptions.builder()
                        .origin(S3Origin.Builder.create(s3Bucket)
                                .originAccessIdentity(originAccessIdentity)
                                .build())
                        .build())
                .build();

        // Create CloudWatch Dashboard
        Dashboard dashboard = Dashboard.Builder.create(this, "GlammeDashboard")
                .dashboardName("Glamme-Monitoring")
                .build();

        // Output important values
        CfnOutput.Builder.create(this, "VpcId")
                .value(vpc.getVpcId())
                .exportName("VpcId")
                .build();

        CfnOutput.Builder.create(this, "AuthServiceRepoUri")
                .value(authRepo.getRepositoryUri())
                .exportName("AuthServiceRepoUri")
                .build();

        CfnOutput.Builder.create(this, "ImageServiceRepoUri")
                .value(imageRepo.getRepositoryUri())
                .exportName("ImageServiceRepoUri")
                .build();

        CfnOutput.Builder.create(this, "MatchingServiceRepoUri")
                .value(matchingRepo.getRepositoryUri())
                .exportName("MatchingServiceRepoUri")
                .build();

        CfnOutput.Builder.create(this, "SocialServiceRepoUri")
                .value(socialRepo.getRepositoryUri())
                .exportName("SocialServiceRepoUri")
                .build();

        CfnOutput.Builder.create(this, "ShoppingServiceRepoUri")
                .value(shoppingRepo.getRepositoryUri())
                .exportName("ShoppingServiceRepoUri")
                .build();

        CfnOutput.Builder.create(this, "CommunicationServiceRepoUri")
                .value(communicationRepo.getRepositoryUri())
                .exportName("CommunicationServiceRepoUri")
                .build();

        CfnOutput.Builder.create(this, "DatabaseEndpoint")
                .value(auroraDb.getClusterEndpoint().getHostname())
                .exportName("DatabaseEndpoint")
                .build();

        CfnOutput.Builder.create(this, "RedisEndpoint")
                .value(redisCluster.getRef())
                .exportName("RedisEndpoint")
                .build();

        CfnOutput.Builder.create(this, "OpenSearchEndpoint")
                .value(openSearchDomain.getDomainEndpoint())
                .exportName("OpenSearchEndpoint")
                .build();

        // TODO: Add API Gateway URL output when API Gateway is created
        // CfnOutput.Builder.create(this, "ApiGatewayUrl")
        //         .value(api.getUrl())
        //         .exportName("ApiGatewayUrl")
        //         .build();

        CfnOutput.Builder.create(this, "UserPoolId")
                .value(userPool.getUserPoolId())
                .exportName("UserPoolId")
                .build();

        CfnOutput.Builder.create(this, "UserPoolClientId")
                .value(userPoolClient.getUserPoolClientId())
                .exportName("UserPoolClientId")
                .build();

        CfnOutput.Builder.create(this, "CloudFrontUrl")
                .value(distribution.getDistributionDomainName())
                .exportName("CloudFrontUrl")
                .build();

        // Service URLs
        CfnOutput.Builder.create(this, "AuthServiceUrl")
                .value(authService.getLoadBalancer().getLoadBalancerDnsName())
                .exportName("AuthServiceUrl")
                .build();

        CfnOutput.Builder.create(this, "ImageServiceUrl")
                .value(imageService.getLoadBalancer().getLoadBalancerDnsName())
                .exportName("ImageServiceUrl")
                .build();

        CfnOutput.Builder.create(this, "MatchingServiceUrl")
                .value(matchingService.getLoadBalancer().getLoadBalancerDnsName())
                .exportName("MatchingServiceUrl")
                .build();

        CfnOutput.Builder.create(this, "SocialServiceUrl")
                .value(socialService.getLoadBalancer().getLoadBalancerDnsName())
                .exportName("SocialServiceUrl")
                .build();

        CfnOutput.Builder.create(this, "ShoppingServiceUrl")
                .value(shoppingService.getLoadBalancer().getLoadBalancerDnsName())
                .exportName("ShoppingServiceUrl")
                .build();

        CfnOutput.Builder.create(this, "CommunicationServiceUrl")
                .value(communicationService.getLoadBalancer().getLoadBalancerDnsName())
                .exportName("CommunicationServiceUrl")
                .build();
    }

    private ApplicationLoadBalancedFargateService createFargateService(
            GlammeVpcStack stack, Cluster cluster, Vpc vpc, String serviceName,
            IRepository repository, Map<String, String> envVars, boolean isPublic) {

        ContainerImage containerImage = ContainerImage.fromEcrRepository(repository);

        ApplicationLoadBalancedFargateService service = ApplicationLoadBalancedFargateService.Builder.create(stack, serviceName + "Fargate")
                .cluster(cluster)
                .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
                        .image(containerImage)
                        .containerPort(8080)
                        .environment(envVars)
                        .build())
                .publicLoadBalancer(isPublic)
                .cpu(256)
                .memoryLimitMiB(512)
                .desiredCount(1)
                .build();

        return service;
    }
}