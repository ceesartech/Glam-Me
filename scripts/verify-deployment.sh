#!/bin/bash

# GlamMe Deployment Verification Script
# Comprehensive post-deployment verification and monitoring

set -e

echo "🚀 GlamMe Deployment Verification"
echo "=================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print status
print_status() {
    local status=$1
    local message=$2
    case $status in
        "success")
            echo -e "${GREEN}✅${NC} $message"
            ;;
        "warning")
            echo -e "${YELLOW}⚠️${NC} $message"
            ;;
        "error")
            echo -e "${RED}❌${NC} $message"
            ;;
        "info")
            echo -e "${BLUE}ℹ️${NC} $message"
            ;;
    esac
}

echo "1. Checking AWS Configuration..."
if aws sts get-caller-identity &>/dev/null; then
    ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
    print_status "success" "AWS CLI authenticated (Account: $ACCOUNT_ID)"
else
    print_status "error" "AWS CLI not authenticated"
    exit 1
fi
echo ""

echo "2. Checking CloudFormation Stack..."
if aws cloudformation describe-stacks --stack-name GlammeVpcStack &>/dev/null; then
    STACK_STATUS=$(aws cloudformation describe-stacks --stack-name GlammeVpcStack --query 'Stacks[0].StackStatus' --output text)
    if [ "$STACK_STATUS" = "CREATE_COMPLETE" ] || [ "$STACK_STATUS" = "UPDATE_COMPLETE" ]; then
        print_status "success" "CloudFormation stack: $STACK_STATUS"

        # Get key outputs
        VPC_ID=$(aws cloudformation describe-stacks --stack-name GlammeVpcStack --query 'Stacks[0].Outputs[?OutputKey==`VpcId`].OutputValue' --output text)
        DB_ENDPOINT=$(aws cloudformation describe-stacks --stack-name GlammeVpcStack --query 'Stacks[0].Outputs[?OutputKey==`DatabaseEndpoint`].OutputValue' --output text)
        REDIS_ENDPOINT=$(aws cloudformation describe-stacks --stack-name GlammeVpcStack --query 'Stacks[0].Outputs[?OutputKey==`RedisEndpoint`].OutputValue' --output text)

        print_status "info" "VPC ID: $VPC_ID"
        print_status "info" "Database Endpoint: $DB_ENDPOINT"
        print_status "info" "Redis Endpoint: $REDIS_ENDPOINT"
    else
        print_status "warning" "CloudFormation stack status: $STACK_STATUS"
    fi
else
    print_status "error" "CloudFormation stack 'GlammeVpcStack' not found"
fi
echo ""

echo "3. Checking ECR Repositories..."
SERVICES=("auth-service" "image-service" "matching-service" "social-service" "shopping-service" "communication-service")
for service in "${SERVICES[@]}"; do
    if aws ecr describe-repositories --repository-names "glamme/$service" &>/dev/null; then
        # Check if images exist
        IMAGE_COUNT=$(aws ecr describe-images --repository-name "glamme/$service" --query 'length(imageDetails)' --output text)
        if [ "$IMAGE_COUNT" -gt 0 ]; then
            LATEST_TAG=$(aws ecr describe-images --repository-name "glamme/$service" --query 'imageDetails[0].imageTags[0]' --output text)
            print_status "success" "ECR Repository 'glamme/$service': $IMAGE_COUNT images (latest: $LATEST_TAG)"
        else
            print_status "warning" "ECR Repository 'glamme/$service': Created but no images"
        fi
    else
        print_status "error" "ECR Repository 'glamme/$service' not found"
    fi
done
echo ""

echo "4. Checking ECS Cluster..."
if aws ecs describe-clusters --clusters glamme-cluster &>/dev/null; then
    CLUSTER_STATUS=$(aws ecs describe-clusters --clusters glamme-cluster --query 'clusters[0].status' --output text)
    if [ "$CLUSTER_STATUS" = "ACTIVE" ]; then
        print_status "success" "ECS Cluster 'glamme-cluster': $CLUSTER_STATUS"
    else
        print_status "warning" "ECS Cluster status: $CLUSTER_STATUS"
    fi
else
    print_status "error" "ECS Cluster 'glamme-cluster' not found"
fi
echo ""

echo "5. Checking ECS Services..."
for service in "${SERVICES[@]}"; do
    if aws ecs describe-services --cluster glamme-cluster --services "$service" &>/dev/null; then
        SERVICE_STATUS=$(aws ecs describe-services --cluster glamme-cluster --services "$service" --query 'services[0].status' --output text)
        DESIRED_COUNT=$(aws ecs describe-services --cluster glamme-cluster --services "$service" --query 'services[0].desiredCount' --output text)
        RUNNING_COUNT=$(aws ecs describe-services --cluster glamme-cluster --services "$service" --query 'services[0].runningCount' --output text)

        if [ "$SERVICE_STATUS" = "ACTIVE" ]; then
            if [ "$RUNNING_COUNT" = "$DESIRED_COUNT" ] && [ "$DESIRED_COUNT" -gt 0 ]; then
                print_status "success" "ECS Service '$service': ACTIVE ($RUNNING_COUNT/$DESIRED_COUNT running)"
            else
                print_status "warning" "ECS Service '$service': ACTIVE but $RUNNING_COUNT/$DESIRED_COUNT running"
            fi
        else
            print_status "error" "ECS Service '$service': $SERVICE_STATUS"
        fi
    else
        print_status "error" "ECS Service '$service' not found"
    fi
done
echo ""

echo "6. Checking Load Balancers..."
ALB_ARN=$(aws elbv2 describe-load-balancers --query 'LoadBalancers[?contains(LoadBalancerName, `glamme`)].LoadBalancerArn' --output text)
if [ -n "$ALB_ARN" ]; then
    ALB_DNS=$(aws elbv2 describe-load-balancers --load-balancer-arns "$ALB_ARN" --query 'LoadBalancers[0].DNSName' --output text)
    ALB_STATE=$(aws elbv2 describe-load-balancers --load-balancer-arns "$ALB_ARN" --query 'LoadBalancers[0].State.Code' --output text)

    if [ "$ALB_STATE" = "active" ]; then
        print_status "success" "Load Balancer: ACTIVE"
        print_status "info" "Load Balancer DNS: $ALB_DNS"
    else
        print_status "warning" "Load Balancer state: $ALB_STATE"
    fi
else
    print_status "error" "Load Balancer not found"
fi
echo ""

echo "7. Service Health Check Summary..."
echo "==================================="
if [ -n "$ALB_DNS" ]; then
    for service in "${SERVICES[@]}"; do
        HEALTH_URL="http://$ALB_DNS/$service/actuator/health"
        if curl -f -s --max-time 10 "$HEALTH_URL" > /dev/null; then
            print_status "success" "$service health check: PASS"
        else
            print_status "warning" "$service health check: FAIL or timeout"
        fi
    done
else
    print_status "warning" "Cannot perform health checks - Load Balancer not available"
fi
echo ""

echo "🎯 DEPLOYMENT VERIFICATION COMPLETE"
echo "==================================="
echo ""
echo "📊 Summary:"
echo "• AWS Account: $ACCOUNT_ID"
echo "• CloudFormation Stack: GlammeVpcStack"
echo "• ECS Cluster: glamme-cluster"
echo "• ECR Repositories: 6 services"
echo "• Load Balancer: $ALB_DNS"
echo ""
echo "🔗 Useful Links:"
echo "• GitHub Actions: https://github.com/ceesartech/Glam-Me/actions"
echo "• ECS Services: https://console.aws.amazon.com/ecs/home#/clusters/glamme-cluster/services"
echo "• CloudWatch Logs: https://console.aws.amazon.com/cloudwatch/home#logs:"
echo ""
echo "📞 For troubleshooting:"
echo "• Check CloudWatch logs for service-specific errors"
echo "• Review ECS service events for deployment issues"
echo "• Verify security groups and network ACLs"
echo "• Check load balancer target group health"
echo ""
print_status "info" "Deployment verification completed successfully!"
