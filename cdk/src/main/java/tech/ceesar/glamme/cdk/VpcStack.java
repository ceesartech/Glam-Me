package tech.ceesar.glamme.cdk;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.CfnOutput;
import software.constructs.Construct;

public class VpcStack extends Stack {
    private final Vpc vpc;
    private final SecurityGroup albSecurityGroup;
    private final SecurityGroup ecsSecurityGroup;
    private final SecurityGroup dbSecurityGroup;
    private final SecurityGroup redisSecurityGroup;
    private final SecurityGroup opensearchSecurityGroup;

    public VpcStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Create VPC with 3 AZs, 6 subnets (3 public, 3 private)
        this.vpc = Vpc.Builder.create(this, "GlammeVpc")
                .maxAzs(3)
                .subnetConfiguration(java.util.Arrays.asList(
                        SubnetConfiguration.builder()
                                .name("Public")
                                .subnetType(SubnetType.PUBLIC)
                                .cidrMask(24)
                                .build(),
                        SubnetConfiguration.builder()
                                .name("Private-App")
                                .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                                .cidrMask(24)
                                .build(),
                        SubnetConfiguration.builder()
                                .name("Private-Data")
                                .subnetType(SubnetType.PRIVATE_ISOLATED)
                                .cidrMask(24)
                                .build()
                ))
                .natGateways(3)
                .build();

        // Security Groups
        this.albSecurityGroup = SecurityGroup.Builder.create(this, "AlbSecurityGroup")
                .vpc(vpc)
                .description("Security group for ALB")
                .build();

        this.albSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(80), "Allow HTTP");
        this.albSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(443), "Allow HTTPS");

        this.ecsSecurityGroup = SecurityGroup.Builder.create(this, "EcsSecurityGroup")
                .vpc(vpc)
                .description("Security group for ECS services")
                .build();

        this.dbSecurityGroup = SecurityGroup.Builder.create(this, "DatabaseSecurityGroup")
                .vpc(vpc)
                .description("Security group for Aurora database")
                .build();

        this.redisSecurityGroup = SecurityGroup.Builder.create(this, "RedisSecurityGroup")
                .vpc(vpc)
                .description("Security group for ElastiCache Redis")
                .build();

        this.opensearchSecurityGroup = SecurityGroup.Builder.create(this, "OpenSearchSecurityGroup")
                .vpc(vpc)
                .description("Security group for OpenSearch")
                .build();

        // Allow ECS to communicate with ALB
        this.ecsSecurityGroup.addIngressRule(this.albSecurityGroup, Port.tcp(8080), "Allow ALB to ECS");

        // Allow ECS to communicate with database
        this.dbSecurityGroup.addIngressRule(this.ecsSecurityGroup, Port.tcp(5432), "Allow ECS to PostgreSQL");

        // Allow ECS to communicate with Redis
        this.redisSecurityGroup.addIngressRule(this.ecsSecurityGroup, Port.tcp(6379), "Allow ECS to Redis");

        // Allow ECS to communicate with OpenSearch
        this.opensearchSecurityGroup.addIngressRule(this.ecsSecurityGroup, Port.tcp(443), "Allow ECS to OpenSearch");

        // VPC Endpoints for AWS services
        this.vpc.addGatewayEndpoint("S3Endpoint", GatewayVpcEndpointOptions.builder()
                .service(GatewayVpcEndpointAwsService.S3)
                .build());

        this.vpc.addInterfaceEndpoint("SecretsManagerEndpoint", InterfaceVpcEndpointOptions.builder()
                .service(InterfaceVpcEndpointAwsService.SECRETS_MANAGER)
                .build());

        this.vpc.addInterfaceEndpoint("KmsEndpoint", InterfaceVpcEndpointOptions.builder()
                .service(InterfaceVpcEndpointAwsService.KMS)
                .build());

        this.vpc.addInterfaceEndpoint("CloudWatchLogsEndpoint", InterfaceVpcEndpointOptions.builder()
                .service(InterfaceVpcEndpointAwsService.CLOUDWATCH_LOGS)
                .build());

        this.vpc.addInterfaceEndpoint("EventBridgeEndpoint", InterfaceVpcEndpointOptions.builder()
                .service(InterfaceVpcEndpointAwsService.EVENTBRIDGE)
                .build());

        this.vpc.addInterfaceEndpoint("BedrockEndpoint", InterfaceVpcEndpointOptions.builder()
                .service(InterfaceVpcEndpointAwsService.SAGEMAKER_RUNTIME)
                .build());

        // Outputs
        CfnOutput.Builder.create(this, "VpcId")
                .exportName("VpcId")
                .value(vpc.getVpcId())
                .build();

        CfnOutput.Builder.create(this, "PublicSubnetIds")
                .exportName("PublicSubnetIds")
                .value(String.join(",", vpc.getPublicSubnets().stream()
                        .map(subnet -> subnet.getSubnetId())
                        .toList()))
                .build();

        CfnOutput.Builder.create(this, "PrivateAppSubnetIds")
                .exportName("PrivateAppSubnetIds")
                .value(String.join(",", vpc.getPrivateSubnets().stream()
                        .map(subnet -> subnet.getSubnetId())
                        .toList()))
                .build();

        CfnOutput.Builder.create(this, "PrivateDataSubnetIds")
                .exportName("PrivateDataSubnetIds")
                .value(String.join(",", vpc.getIsolatedSubnets().stream()
                        .map(subnet -> subnet.getSubnetId())
                        .toList()))
                .build();
    }

    public Vpc getVpc() {
        return vpc;
    }

    public SecurityGroup getAlbSecurityGroup() {
        return albSecurityGroup;
    }

    public SecurityGroup getEcsSecurityGroup() {
        return ecsSecurityGroup;
    }

    public SecurityGroup getDbSecurityGroup() {
        return dbSecurityGroup;
    }

    public SecurityGroup getRedisSecurityGroup() {
        return redisSecurityGroup;
    }

    public SecurityGroup getOpensearchSecurityGroup() {
        return opensearchSecurityGroup;
    }
}
