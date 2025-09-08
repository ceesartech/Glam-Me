#!/bin/bash

# Script to create OIDC provider for GitHub Actions
# This script creates the necessary OIDC provider and IAM role for GitHub Actions

set -e

# Configuration
AWS_REGION="us-east-1"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REPO_OWNER="ceesartech"  # Replace with your GitHub username/organization
REPO_NAME="Glam-Me"      # Replace with your repository name
ROLE_NAME="GlamMe-CICD-Role"

echo "🚀 Setting up OIDC provider for GitHub Actions"
echo "Account ID: $ACCOUNT_ID"
echo "Region: $AWS_REGION"
echo "Repository: $REPO_OWNER/$REPO_NAME"

# Step 1: Create OIDC Provider
echo "📋 Creating OIDC provider..."
aws iam create-open-id-connect-provider \
    --url https://token.actions.githubusercontent.com \
    --client-id-list sts.amazonaws.com \
    --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1 \
    --region $AWS_REGION || echo "OIDC provider may already exist"

# Step 2: Create IAM Policy
echo "📋 Creating IAM policy..."
cat > cicd-policy.json << EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ecr:GetAuthorizationToken",
                "ecr:BatchCheckLayerAvailability",
                "ecr:GetDownloadUrlForLayer",
                "ecr:BatchGetImage",
                "ecr:PutImage",
                "ecr:InitiateLayerUpload",
                "ecr:UploadLayerPart",
                "ecr:CompleteLayerUpload",
                "ecr:CreateRepository",
                "ecr:DescribeRepositories",
                "ecr:DescribeImages",
                "ecr:TagResource",
                "ecr:UntagResource"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "ecs:UpdateService",
                "ecs:DescribeServices",
                "ecs:DescribeTaskDefinition",
                "ecs:RegisterTaskDefinition",
                "ecs:ListTasks",
                "ecs:DescribeTasks",
                "ecs:RunTask",
                "ecs:StopTask"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "cloudformation:*",
                "s3:*",
                "iam:PassRole",
                "logs:*",
                "secretsmanager:GetSecretValue",
                "secretsmanager:DescribeSecret"
            ],
            "Resource": "*"
        }
    ]
}
EOF

aws iam create-policy \
    --policy-name GlamMe-CICD-Policy \
    --policy-document file://cicd-policy.json \
    --region $AWS_REGION || echo "Policy may already exist"

# Step 3: Create Trust Policy
echo "📋 Creating trust policy..."
cat > trust-policy.json << EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Federated": "arn:aws:iam::${ACCOUNT_ID}:oidc-provider/token.actions.githubusercontent.com"
            },
            "Action": "sts:AssumeRoleWithWebIdentity",
            "Condition": {
                "StringEquals": {
                    "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
                },
                "StringLike": {
                    "token.actions.githubusercontent.com:sub": "repo:${REPO_OWNER}/${REPO_NAME}:*"
                }
            }
        }
    ]
}
EOF

# Step 4: Create IAM Role
echo "📋 Creating IAM role..."
aws iam create-role \
    --role-name $ROLE_NAME \
    --assume-role-policy-document file://trust-policy.json \
    --region $AWS_REGION || echo "Role may already exist"

# Step 5: Attach Policy to Role
echo "📋 Attaching policy to role..."
aws iam attach-role-policy \
    --role-name $ROLE_NAME \
    --policy-arn arn:aws:iam::${ACCOUNT_ID}:policy/GlamMe-CICD-Policy \
    --region $AWS_REGION || echo "Policy may already be attached"

# Cleanup
rm -f cicd-policy.json trust-policy.json

echo "✅ Setup complete!"
echo ""
echo "📋 Next steps:"
echo "1. Add these secrets to your GitHub repository:"
echo "   AWS_ROLE_ARN=arn:aws:iam::${ACCOUNT_ID}:role/${ROLE_NAME}"
echo "   AWS_REGION=${AWS_REGION}"
echo "   ACCOUNT_ID=${ACCOUNT_ID}"
echo ""
echo "2. Go to GitHub → Settings → Secrets and variables → Actions"
echo "3. Add the secrets above"
echo "4. Push code to trigger the workflow"
echo ""
echo "🔗 OIDC Provider ARN: arn:aws:iam::${ACCOUNT_ID}:oidc-provider/token.actions.githubusercontent.com"
echo "🔗 IAM Role ARN: arn:aws:iam::${ACCOUNT_ID}:role/${ROLE_NAME}"
