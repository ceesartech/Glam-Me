# CDK Deployment Troubleshooting Guide

## Overview
This guide addresses common issues encountered during AWS CDK infrastructure deployment for the GlamMe platform.

## Recent Fixes Applied

### ✅ OpenSearch Subnet Configuration Error

**Problem**: 
```
You must specify exactly one subnet. (Service: OpenSearch, Status Code: 400)
```

**Root Cause**: OpenSearch domain was configured with a VPC but didn't specify which subnets to use.

**Solution Applied**:
- Added `vpcSubnets` configuration to OpenSearch domain
- Specified `PRIVATE_WITH_EGRESS` subnet type
- Fixed type compatibility issue (SubnetSelection vs List<SubnetSelection>)

**Code Fix**:
```java
.vpcSubnets(Arrays.asList(SubnetSelection.builder()
        .subnetType(software.amazon.awscdk.services.ec2.SubnetType.PRIVATE_WITH_EGRESS)
        .build()))
```

### ✅ CDK Bootstrap Permissions

**Problem**: 
```
CDK deployment requires bootstrap stack version '6', but during the confirmation via SSM parameter /cdk-bootstrap/hnb659fds/version the following error occurred: AccessDeniedException: User is not authorized to perform: ssm:GetParameter
```

**Solution Applied**:
- Updated IAM policy with comprehensive SSM permissions
- Added full access to all AWS services needed for CDK deployment
- Created `update-cicd-permissions.sh` script for future reference

## Infrastructure Configuration

### VPC Setup
- **Availability Zones**: 3 AZs for high availability
- **Subnets**: 6 subnets total
  - 3 Public subnets (for ALB, NAT Gateways)
  - 3 Private-App subnets (for ECS services)
  - 3 Private-Data subnets (for databases, OpenSearch)
- **NAT Gateways**: 3 (one per AZ for redundancy)

### Security Groups
- **ALB Security Group**: Allows HTTP (80) and HTTPS (443)
- **ECS Security Group**: Allows ALB to ECS (8080)
- **Database Security Group**: Allows ECS to PostgreSQL (5432)
- **Redis Security Group**: Allows ECS to Redis (6379)
- **OpenSearch Security Group**: Allows ECS to OpenSearch (443)

### Services Configuration
- **Aurora PostgreSQL**: Serverless v2, version 15.4
- **ElastiCache Redis**: Single node, t3.micro
- **OpenSearch**: Single data node, t3.small.search, 20GB GP3 storage
- **ECS**: Fargate with Application Load Balancer
- **S3**: Versioned bucket with S3-managed encryption

## Common Deployment Issues

### 1. CDK Bootstrap Required
**Error**: `CDK deployment requires bootstrap stack version`
**Solution**: 
```bash
cdk bootstrap aws://476114151082/us-east-1
```

### 2. Permission Denied Errors
**Error**: `User is not authorized to perform`
**Solution**: Run the permissions update script:
```bash
./scripts/update-cicd-permissions.sh
```

### 3. VPC Configuration Issues
**Error**: `Invalid VPC configuration`
**Solution**: Ensure VPC is deployed first:
```bash
cdk deploy GlammeVpcStack
```

### 4. OpenSearch Subnet Errors
**Error**: `You must specify exactly one subnet`
**Solution**: ✅ Fixed - OpenSearch now specifies subnets correctly

### 5. Resource Limit Exceeded
**Error**: `Resource limit exceeded`
**Solution**: 
- Check AWS service limits in your account
- Use smaller instance types for development
- Consider using different regions

## Deployment Steps

### Prerequisites
1. AWS CLI configured with appropriate credentials
2. CDK installed: `npm install -g aws-cdk`
3. Java 17+ installed
4. Gradle installed

### Step-by-Step Deployment

1. **Bootstrap CDK** (if not already done):
   ```bash
   cdk bootstrap aws://476114151082/us-east-1
   ```

2. **Build CDK**:
   ```bash
   cd cdk
   ./gradlew build
   ```

3. **Deploy VPC Stack**:
   ```bash
   cdk deploy GlammeVpcStack --require-approval never
   ```

4. **Deploy Main Infrastructure**:
   ```bash
   cdk deploy GlammeCdkApp --require-approval never
   ```

### Verification
After deployment, verify:
- VPC and subnets created correctly
- Security groups configured properly
- Aurora database accessible
- Redis cluster running
- OpenSearch domain accessible
- ECS cluster created
- S3 bucket accessible

## Troubleshooting Commands

### Check CDK Status
```bash
cdk list
cdk diff
cdk synth
```

### Check AWS Resources
```bash
aws ec2 describe-vpcs
aws ec2 describe-subnets
aws rds describe-db-clusters
aws elasticache describe-cache-clusters
aws opensearch describe-domains
aws ecs describe-clusters
```

### View CloudFormation Stacks
```bash
aws cloudformation list-stacks
aws cloudformation describe-stacks --stack-name GlammeVpcStack
```

## Monitoring and Logs

### CloudFormation Events
- Check CloudFormation console for detailed error messages
- Look for specific resource creation failures
- Review rollback reasons

### CloudWatch Logs
- ECS service logs: `/ecs/glamme-cluster`
- Lambda function logs: `/aws/lambda/glamme-*`
- API Gateway logs: `/aws/apigateway/glamme-*`

### AWS Service Health
- Check AWS Service Health Dashboard
- Verify service availability in your region
- Monitor AWS Personal Health Dashboard

## Best Practices

### Security
- Use least privilege IAM policies
- Enable VPC Flow Logs for network monitoring
- Use AWS Secrets Manager for sensitive data
- Enable CloudTrail for API auditing

### Cost Optimization
- Use appropriate instance sizes
- Enable auto-scaling where possible
- Use Spot instances for non-critical workloads
- Monitor costs with AWS Cost Explorer

### Reliability
- Deploy across multiple AZs
- Use managed services where possible
- Implement proper backup strategies
- Set up monitoring and alerting

## Support and Resources

### Documentation
- [AWS CDK Developer Guide](https://docs.aws.amazon.com/cdk/)
- [AWS CDK API Reference](https://docs.aws.amazon.com/cdk/api/)
- [AWS Service Documentation](https://docs.aws.amazon.com/)

### Community
- [AWS CDK GitHub](https://github.com/aws/aws-cdk)
- [AWS CDK Slack](https://cdk-dev.slack.com/)
- [AWS Forums](https://forums.aws.amazon.com/)

### Scripts Available
- `scripts/troubleshoot-cdk-deployment.sh` - General CDK troubleshooting
- `scripts/update-cicd-permissions.sh` - Fix IAM permissions
- `scripts/setup-oidc-provider.sh` - Setup GitHub Actions OIDC
- `scripts/fix-ecr-permissions.sh` - Fix ECR permissions
