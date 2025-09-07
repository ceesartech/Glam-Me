#!/bin/bash

# GlamMe Deployment Script
# Handles Docker image building, ECR push, and deployment

set -e

# Configuration
AWS_REGION="us-east-1"
AWS_PROFILE="default"
ACCOUNT_ID="476114151082"
ECR_BASE_URL="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

SERVICES=(
    "auth-service"
    "image-service"
    "matching-service"
    "social-service"
    "shopping-service"
    "communication-service"
)

echo "🚀 Starting GlamMe Deployment..."

# Function to authenticate with ECR
authenticate_ecr() {
    echo "🔐 Authenticating with ECR..."
    aws ecr get-login-password --region $AWS_REGION --profile $AWS_PROFILE | docker login --username AWS --password-stdin $ECR_BASE_URL
}

# Function to push service image
push_service_image() {
    local service=$1
    local max_retries=3
    local retry_count=0

    echo "📤 Pushing $service image..."

    while [ $retry_count -lt $max_retries ]; do
        if docker tag glam-me/$service:latest $ECR_BASE_URL/glamme/$service:latest && \
           docker push $ECR_BASE_URL/glamme/$service:latest; then
            echo "✅ Successfully pushed $service"
            return 0
        else
            retry_count=$((retry_count + 1))
            echo "❌ Failed to push $service (attempt $retry_count/$max_retries)"
            if [ $retry_count -lt $max_retries ]; then
                echo "⏳ Retrying in 5 seconds..."
                sleep 5
            fi
        fi
    done

    echo "💥 Failed to push $service after $max_retries attempts"
    return 1
}

# Main deployment process
echo "🔍 Checking Docker images..."
docker images | grep glam-me || {
    echo "❌ Docker images not found. Please build them first."
    exit 1
}

echo "🔐 Authenticating with ECR..."
authenticate_ecr

echo "📤 Pushing Docker images to ECR..."
failed_services=()

for service in "${SERVICES[@]}"; do
    if ! push_service_image "$service"; then
        failed_services+=("$service")
    fi
done

if [ ${#failed_services[@]} -eq 0 ]; then
    echo "🎉 All services pushed successfully!"
    echo ""
    echo "🚀 Next steps:"
    echo "1. Update CDK to deploy ECS services"
    echo "2. Configure environment variables"
    echo "3. Test service deployments"
else
    echo "⚠️  Some services failed to push: ${failed_services[*]}"
    echo ""
    echo "🔄 You can retry individual services:"
    for service in "${failed_services[@]}"; do
        echo "   ./deploy.sh retry $service"
    done
fi
