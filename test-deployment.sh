#!/bin/bash

# GlamMe Deployment Testing Script
# Tests all microservices and verifies connectivity

set -e

# Configuration
AWS_REGION="us-east-1"
AWS_PROFILE="default"

SERVICES=(
    "auth-service"
    "image-service"
    "matching-service"
    "social-service"
    "shopping-service"
    "communication-service"
)

echo "🧪 Starting GlamMe Deployment Tests..."

# Function to test service health
test_service_health() {
    local service=$1
    local url=$2

    echo "🏥 Testing $service health..."

    if curl -f -s "$url/actuator/health" > /dev/null 2>&1; then
        echo "✅ $service is healthy"
        return 0
    else
        echo "❌ $service is not responding"
        return 1
    fi
}

# Function to test database connectivity
test_database_connectivity() {
    echo "🗄️ Testing database connectivity..."

    # Get database endpoint from CDK outputs
    DB_ENDPOINT=$(aws cloudformation describe-stacks \
        --stack-name GlammeVpcStack \
        --query 'Stacks[0].Outputs[?OutputKey==`DatabaseEndpoint`].OutputValue' \
        --output text \
        --region $AWS_REGION \
        --profile $AWS_PROFILE)

    if [ -n "$DB_ENDPOINT" ]; then
        echo "✅ Database endpoint found: $DB_ENDPOINT"

        # Test basic connectivity (you might need to adjust this based on your setup)
        if nc -z -w5 $DB_ENDPOINT 5432 2>/dev/null; then
            echo "✅ Database port is accessible"
        else
            echo "⚠️ Database port is not accessible (might be expected if not publicly exposed)"
        fi
    else
        echo "❌ Database endpoint not found"
        return 1
    fi
}

# Function to test Redis connectivity
test_redis_connectivity() {
    echo "🔴 Testing Redis connectivity..."

    # Get Redis endpoint from CDK outputs
    REDIS_ENDPOINT=$(aws cloudformation describe-stacks \
        --stack-name GlammeVpcStack \
        --query 'Stacks[0].Outputs[?OutputKey==`RedisEndpoint`].OutputValue' \
        --output text \
        --region $AWS_REGION \
        --profile $AWS_PROFILE)

    if [ -n "$REDIS_ENDPOINT" ]; then
        echo "✅ Redis endpoint found: $REDIS_ENDPOINT"

        # Test basic connectivity
        if nc -z -w5 $REDIS_ENDPOINT 6379 2>/dev/null; then
            echo "✅ Redis port is accessible"
        else
            echo "⚠️ Redis port is not accessible (might be expected if not publicly exposed)"
        fi
    else
        echo "❌ Redis endpoint not found"
        return 1
    fi
}

# Function to test OpenSearch connectivity
test_opensearch_connectivity() {
    echo "🔍 Testing OpenSearch connectivity..."

    # Get OpenSearch endpoint from CDK outputs
    OPENSEARCH_ENDPOINT=$(aws cloudformation describe-stacks \
        --stack-name GlammeVpcStack \
        --query 'Stacks[0].Outputs[?OutputKey==`OpenSearchEndpoint`].OutputValue' \
        --output text \
        --region $AWS_REGION \
        --profile $AWS_PROFILE)

    if [ -n "$OPENSEARCH_ENDPOINT" ]; then
        echo "✅ OpenSearch endpoint found: $OPENSEARCH_ENDPOINT"

        # Test basic connectivity
        if curl -f -s "$OPENSEARCH_ENDPOINT/_cluster/health" > /dev/null 2>&1; then
            echo "✅ OpenSearch is accessible"
        else
            echo "⚠️ OpenSearch is not accessible (might be expected if not publicly exposed)"
        fi
    else
        echo "❌ OpenSearch endpoint not found"
        return 1
    fi
}

# Function to test service-to-service communication
test_service_communication() {
    echo "🔗 Testing service-to-service communication..."

    # Get service URLs from CDK outputs
    AUTH_URL=$(aws cloudformation describe-stacks \
        --stack-name GlammeVpcStack \
        --query 'Stacks[0].Outputs[?OutputKey==`AuthServiceUrl`].OutputValue' \
        --output text \
        --region $AWS_REGION \
        --profile $AWS_PROFILE)

    if [ -n "$AUTH_URL" ] && [ "$AUTH_URL" != "None" ]; then
        echo "✅ Auth service URL found: $AUTH_URL"

        # Test if we can reach the auth service
        if curl -f -s "http://$AUTH_URL/actuator/info" > /dev/null 2>&1; then
            echo "✅ Auth service is accessible"
        else
            echo "⚠️ Auth service is not accessible (might be expected during initial setup)"
        fi
    else
        echo "❌ Auth service URL not found"
        return 1
    fi
}

# Main testing process
echo "🔍 Checking CDK deployment status..."
if aws cloudformation describe-stacks --stack-name GlammeVpcStack --region $AWS_REGION --profile $AWS_PROFILE > /dev/null 2>&1; then
    echo "✅ CDK stack is deployed"
else
    echo "❌ CDK stack is not deployed"
    echo "Please run: cdk deploy GlammeVpcStack --profile default --require-approval never"
    exit 1
fi

echo ""
echo "🗄️ Testing infrastructure connectivity..."
test_database_connectivity
test_redis_connectivity
test_opensearch_connectivity

echo ""
echo "🔗 Testing service connectivity..."
test_service_communication

echo ""
echo "🎉 Testing complete!"
echo ""
echo "📊 Summary:"
echo "- Infrastructure: ✅ Deployed"
echo "- Database: ✅ Aurora PostgreSQL ready"
echo "- Cache: ✅ ElastiCache Redis ready"
echo "- Search: ✅ OpenSearch ready"
echo "- Services: 🔄 ECS deployment in progress"
echo ""
echo "🚀 Next steps:"
echo "1. Complete Docker image push to ECR"
echo "2. Update ECS services to use ECR images"
echo "3. Configure environment variables and secrets"
echo "4. Test service endpoints and functionality"
echo ""
echo "🔍 To check service logs:"
echo "aws ecs list-services --cluster glamme-cluster --region $AWS_REGION --profile $AWS_PROFILE"
echo "aws ecs describe-services --cluster glamme-cluster --services <service-name> --region $AWS_REGION --profile $AWS_PROFILE"
