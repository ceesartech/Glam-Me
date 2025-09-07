#!/bin/bash

# Setup AWS Secrets and Parameters for GlamMe
# This script creates the necessary secrets in AWS Secrets Manager

set -e

# Configuration
AWS_REGION="us-east-1"
AWS_PROFILE="default"
ENVIRONMENT="prod"

echo "🔐 Setting up AWS Secrets for GlamMe..."

# Create database password secret
echo "📝 Creating database password secret..."
aws secretsmanager create-secret \
    --name "/glamme/${ENVIRONMENT}/database/password" \
    --description "Database password for GlamMe Aurora PostgreSQL" \
    --secret-string '{"password":"CHANGE_THIS_PASSWORD"}' \
    --region $AWS_REGION \
    --profile $AWS_PROFILE \
    --tags '[{"Key":"Environment","Value":"'"${ENVIRONMENT}"'"},{"Key":"Service","Value":"database"}]'

# Create JWT secret
echo "🔑 Creating JWT secret..."
aws secretsmanager create-secret \
    --name "/glamme/${ENVIRONMENT}/jwt/secret" \
    --description "JWT signing secret for GlamMe authentication" \
    --secret-string '{"secret":"CHANGE_THIS_TO_A_256_BIT_RANDOM_SECRET"}' \
    --region $AWS_REGION \
    --profile $AWS_PROFILE \
    --tags '[{"Key":"Environment","Value":"'"${ENVIRONMENT}"'"},{"Key":"Service","Value":"auth"}]'

# Create Stripe configuration
echo "💳 Creating Stripe configuration..."
aws secretsmanager create-secret \
    --name "/glamme/${ENVIRONMENT}/stripe/config" \
    --description "Stripe API keys for GlamMe payment processing" \
    --secret-string '{"api_key":"sk_test_CHANGE_THIS","webhook_secret":"whsec_CHANGE_THIS"}' \
    --region $AWS_REGION \
    --profile $AWS_PROFILE \
    --tags '[{"Key":"Environment","Value":"'"${ENVIRONMENT}"'"},{"Key":"Service","Value":"shopping"}]'

# Create EasyPost configuration
echo "📦 Creating EasyPost configuration..."
aws secretsmanager create-secret \
    --name "/glamme/${ENVIRONMENT}/easypost/config" \
    --description "EasyPost API key for GlamMe shipping" \
    --secret-string '{"api_key":"EZTK_CHANGE_THIS"}' \
    --region $AWS_REGION \
    --profile $AWS_PROFILE \
    --tags '[{"Key":"Environment","Value":"'"${ENVIRONMENT}"'"},{"Key":"Service","Value":"shopping"}]'

# Create OpenAI configuration
echo "🤖 Creating OpenAI configuration..."
aws secretsmanager create-secret \
    --name "/glamme/${ENVIRONMENT}/openai/config" \
    --description "OpenAI API key for GlamMe image processing" \
    --secret-string '{"api_key":"sk-CHANGE_THIS"}' \
    --region $AWS_REGION \
    --profile $AWS_PROFILE \
    --tags '[{"Key":"Environment","Value":"'"${ENVIRONMENT}"'"},{"Key":"Service","Value":"image"}]'

# Create OAuth configuration
echo "🔗 Creating OAuth configuration..."
aws secretsmanager create-secret \
    --name "/glamme/${ENVIRONMENT}/oauth/google" \
    --description "Google OAuth credentials for GlamMe authentication" \
    --secret-string '{"client_id":"CHANGE_THIS","client_secret":"CHANGE_THIS"}' \
    --region $AWS_REGION \
    --profile $AWS_PROFILE \
    --tags '[{"Key":"Environment","Value":"'"${ENVIRONMENT}"'"},{"Key":"Service","Value":"auth"}]'

echo "✅ AWS Secrets setup complete!"
echo ""
echo "📋 Next steps:"
echo "1. Update the secret values in AWS Secrets Manager with real credentials"
echo "2. Update CDK to reference these secrets"
echo "3. Deploy ECS services with proper environment variables"
echo ""
echo "🔍 To view created secrets:"
echo "aws secretsmanager list-secrets --region $AWS_REGION --profile $AWS_PROFILE --filters Key=tag:Environment,Values=$ENVIRONMENT"
