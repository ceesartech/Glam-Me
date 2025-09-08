# GitHub Actions OIDC Setup Guide

## Problem
You're getting this error:
```
Could not assume role with OIDC: No OpenIDConnect provider found in your account for https://token.actions.githubusercontent.com
```

## Solution
This error occurs because your AWS account doesn't have an OIDC provider configured for GitHub Actions. Here's how to fix it:

## Quick Fix (Automated)

Run the setup script:
```bash
./scripts/setup-oidc-provider.sh
```

## Manual Setup (Step by Step)

### Step 1: Create OIDC Provider

```bash
aws iam create-open-id-connect-provider \
    --url https://token.actions.githubusercontent.com \
    --client-id-list sts.amazonaws.com \
    --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1
```

### Step 2: Create IAM Policy

Create a file called `cicd-policy.json`:

```json
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
```

Then create the policy:
```bash
aws iam create-policy \
    --policy-name GlamMe-CICD-Policy \
    --policy-document file://cicd-policy.json
```

### Step 3: Create Trust Policy

Create a file called `trust-policy.json` (replace ACCOUNT_ID with your actual account ID):

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Federated": "arn:aws:iam::ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com"
            },
            "Action": "sts:AssumeRoleWithWebIdentity",
            "Condition": {
                "StringEquals": {
                    "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
                },
                "StringLike": {
                    "token.actions.githubusercontent.com:sub": "repo:ceesartech/Glam-Me:*"
                }
            }
        }
    ]
}
```

### Step 4: Create IAM Role

```bash
aws iam create-role \
    --role-name GlamMe-CICD-Role \
    --assume-role-policy-document file://trust-policy.json
```

### Step 5: Attach Policy to Role

```bash
aws iam attach-role-policy \
    --role-name GlamMe-CICD-Role \
    --policy-arn arn:aws:iam::ACCOUNT_ID:policy/GlamMe-CICD-Policy
```

## Configure GitHub Secrets

After creating the OIDC provider and IAM role, add these secrets to your GitHub repository:

1. Go to your GitHub repository
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Add these repository secrets:

```
AWS_ROLE_ARN=arn:aws:iam::476114151082:role/GlamMe-CICD-Role
AWS_REGION=us-east-1
ACCOUNT_ID=476114151082
```

## Verify Setup

You can verify the OIDC provider was created:

```bash
aws iam list-open-id-connect-providers
```

You should see:
```
arn:aws:iam::476114151082:oidc-provider/token.actions.githubusercontent.com
```

## Test the Pipeline

After setting up the OIDC provider and GitHub secrets:

1. Push code to your repository
2. Go to the **Actions** tab in GitHub
3. Check if the workflow runs successfully

## Troubleshooting

### Common Issues

1. **Wrong Repository Name**: Make sure the `sub` condition in the trust policy matches your repository name exactly
2. **Missing Permissions**: Ensure the IAM policy has all necessary permissions for ECR, ECS, and CloudFormation
3. **Region Mismatch**: Make sure the AWS region in GitHub secrets matches your AWS CLI configuration

### Verify Trust Policy

Check that your trust policy allows the correct repository:

```bash
aws iam get-role --role-name GlamMe-CICD-Role
```

The trust policy should include:
```json
"token.actions.githubusercontent.com:sub": "repo:ceesartech/Glam-Me:*"
```

## Security Notes

- The OIDC provider only trusts GitHub Actions tokens
- The trust policy restricts access to your specific repository
- No long-term AWS credentials are stored in GitHub
- Tokens are short-lived and automatically rotated
