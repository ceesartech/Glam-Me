#!/bin/bash

# Setup CI/CD IAM Role and Policies for GlamMe
# This script creates the necessary IAM resources for GitHub Actions CI/CD

set -e

# Configuration
AWS_REGION="us-east-1"
AWS_PROFILE="default"
ACCOUNT_ID="476114151082"
ROLE_NAME="GlamMe-CICD-Role"
REPO_NAME="chijiokeekechi/GlamMe"

echo "🚀 Setting up CI/CD IAM Role for GlamMe..."

# Step 1: Create the IAM policy
echo "📋 Creating IAM policy..."
POLICY_ARN=$(aws iam create-policy \
    --policy-name GlamMe-CICD-Policy \
    --policy-document file://iac/cicd-iam-policy.json \
    --description "IAM policy for GlamMe CI/CD pipeline" \
    --region $AWS_REGION \
    --profile $AWS_PROFILE \
    --query 'Policy.Arn' \
    --output text)

echo "✅ Created IAM policy: $POLICY_ARN"

# Step 2: Create the IAM role
echo "👤 Creating IAM role..."
aws iam create-role \
    --role-name $ROLE_NAME \
    --assume-role-policy-document file://iac/cicd-trust-policy.json \
    --description "IAM role for GlamMe GitHub Actions CI/CD" \
    --region $AWS_REGION \
    --profile $AWS_PROFILE

echo "✅ Created IAM role: $ROLE_NAME"

# Step 3: Attach the policy to the role
echo "🔗 Attaching policy to role..."
aws iam attach-role-policy \
    --role-name $ROLE_NAME \
    --policy-arn $POLICY_ARN \
    --region $AWS_REGION \
    --profile $AWS_PROFILE

echo "✅ Attached policy to role"

# Step 4: Get the role ARN
ROLE_ARN=$(aws iam get-role \
    --role-name $ROLE_NAME \
    --region $AWS_REGION \
    --profile $AWS_PROFILE \
    --query 'Role.Arn' \
    --output text)

echo ""
echo "🎉 CI/CD Setup Complete!"
echo "================================"
echo ""
echo "📋 Summary:"
echo "   Role Name: $ROLE_NAME"
echo "   Role ARN: $ROLE_ARN"
echo "   Policy ARN: $POLICY_ARN"
echo ""
echo "🔧 Next Steps:"
echo "1. Add the Role ARN to GitHub repository secrets:"
echo "   - Go to: https://github.com/$REPO_NAME/settings/secrets/actions"
echo "   - Add secret: AWS_ROLE_ARN = $ROLE_ARN"
echo ""
echo "2. Add additional secrets if needed:"
echo "   - AWS_REGION = $AWS_REGION"
echo "   - ACCOUNT_ID = $ACCOUNT_ID"
echo ""
echo "3. Push this code to GitHub to trigger the CI/CD pipeline"
echo ""
echo "4. The pipeline will automatically:"
echo "   - Build all microservices"
echo "   - Run tests"
echo "   - Build Docker images"
echo "   - Push to ECR"
echo "   - Deploy infrastructure"
echo "   - Deploy ECS services"
echo ""
echo "📚 Pipeline Status:"
echo "   View at: https://github.com/$REPO_NAME/actions"
echo ""
echo "🎯 Manual Pipeline Trigger:"
echo "   Go to Actions tab → Deploy workflow → Run workflow"
echo ""
echo "✅ Your GlamMe CI/CD pipeline is ready for automated deployments!"
