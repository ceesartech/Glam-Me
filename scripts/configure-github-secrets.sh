#!/bin/bash

# Script to help configure GitHub secrets for CI/CD
# This script outputs the secrets you need to add to GitHub

set -e

# Configuration
AWS_REGION="us-east-1"
ACCOUNT_ID="476114151082"
ROLE_NAME="GlamMe-CICD-Role"

echo "🔐 GitHub Secrets Configuration"
echo "================================"
echo ""
echo "Add these secrets to your GitHub repository:"
echo ""
echo "1. Go to GitHub → Settings → Secrets and variables → Actions"
echo "2. Click 'New repository secret'"
echo "3. Add each secret below:"
echo ""
echo "Secret Name: AWS_ROLE_ARN"
echo "Secret Value: arn:aws:iam::${ACCOUNT_ID}:role/${ROLE_NAME}"
echo ""
echo "Secret Name: AWS_REGION"
echo "Secret Value: ${AWS_REGION}"
echo ""
echo "Secret Name: ACCOUNT_ID"
echo "Secret Value: ${ACCOUNT_ID}"
echo ""
echo "📋 Copy-paste commands for GitHub CLI (if you have it installed):"
echo ""
echo "gh secret set AWS_ROLE_ARN --body 'arn:aws:iam::${ACCOUNT_ID}:role/${ROLE_NAME}'"
echo "gh secret set AWS_REGION --body '${AWS_REGION}'"
echo "gh secret set ACCOUNT_ID --body '${ACCOUNT_ID}'"
echo ""
echo "✅ After adding these secrets, your GitHub Actions should work!"
echo ""
echo "🔗 OIDC Provider: arn:aws:iam::${ACCOUNT_ID}:oidc-provider/token.actions.githubusercontent.com"
echo "🔗 IAM Role: arn:aws:iam::${ACCOUNT_ID}:role/${ROLE_NAME}"
