#!/bin/bash

# Script to update CI/CD permissions for comprehensive AWS deployment
# This script creates a streamlined policy with all necessary permissions for CDK deployment

set -e

POLICY_ARN="arn:aws:iam::476114151082:policy/GlamMe-CICD-Policy"

echo "🔧 Updating CI/CD permissions for comprehensive AWS deployment"
echo "=============================================================="
echo ""

# Create the streamlined policy document
cat > streamlined-cicd-policy.json << 'EOF'
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "ECRGlobalAccess",
            "Effect": "Allow",
            "Action": [
                "ecr:GetAuthorizationToken"
            ],
            "Resource": "*"
        },
        {
            "Sid": "ECRAccess",
            "Effect": "Allow",
            "Action": [
                "ecr:BatchCheckLayerAvailability",
                "ecr:GetDownloadUrlForLayer",
                "ecr:GetRepositoryPolicy",
                "ecr:DescribeRepositories",
                "ecr:ListImages",
                "ecr:DescribeImages",
                "ecr:BatchGetImage",
                "ecr:GetLifecycleConfiguration",
                "ecr:GetLifecyclePolicy",
                "ecr:GetLifecyclePolicyPreview",
                "ecr:ListTagsForResource",
                "ecr:DescribeImageScanFindings",
                "ecr:InitiateLayerUpload",
                "ecr:UploadLayerPart",
                "ecr:CompleteLayerUpload",
                "ecr:PutImage",
                "ecr:CreateRepository",
                "ecr:TagResource",
                "ecr:UntagResource"
            ],
            "Resource": [
                "arn:aws:ecr:us-east-1:476114151082:repository/glamme/*"
            ]
        },
        {
            "Sid": "ECSDeployment",
            "Effect": "Allow",
            "Action": [
                "ecs:UpdateService",
                "ecs:DescribeServices",
                "ecs:DescribeTasks",
                "ecs:ListTasks",
                "ecs:DescribeTaskDefinition",
                "ecs:RegisterTaskDefinition",
                "ecs:DeregisterTaskDefinition",
                "ecs:CreateTaskSet",
                "ecs:UpdateTaskSet",
                "ecs:DeleteTaskSet",
                "ecs:DescribeTaskSets",
                "ecs:UpdateServicePrimaryTaskSet",
                "ecs:UpdateServiceForceNewDeployment",
                "ecs:Waiter"
            ],
            "Resource": [
                "arn:aws:ecs:us-east-1:476114151082:cluster/glamme-cluster",
                "arn:aws:ecs:us-east-1:476114151082:service/glamme-cluster/*"
            ]
        },
        {
            "Sid": "CloudFormationDeployment",
            "Effect": "Allow",
            "Action": [
                "cloudformation:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "IAMCDK",
            "Effect": "Allow",
            "Action": [
                "iam:PassRole",
                "iam:GetRole",
                "iam:CreateRole",
                "iam:AttachRolePolicy",
                "iam:DetachRolePolicy",
                "iam:DeleteRole",
                "iam:GetPolicy",
                "iam:CreatePolicy",
                "iam:DeletePolicy",
                "iam:ListAttachedRolePolicies",
                "iam:ListRolePolicies",
                "iam:PutRolePolicy",
                "iam:DeleteRolePolicy",
                "iam:GetRolePolicy"
            ],
            "Resource": "*"
        },
        {
            "Sid": "S3Deployment",
            "Effect": "Allow",
            "Action": [
                "s3:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "SSMAccess",
            "Effect": "Allow",
            "Action": [
                "ssm:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "SecretsManager",
            "Effect": "Allow",
            "Action": [
                "secretsmanager:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "LogsAccess",
            "Effect": "Allow",
            "Action": [
                "logs:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "CloudWatchAccess",
            "Effect": "Allow",
            "Action": [
                "cloudwatch:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "EC2Access",
            "Effect": "Allow",
            "Action": [
                "ec2:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "LambdaAccess",
            "Effect": "Allow",
            "Action": [
                "lambda:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "APIGatewayAccess",
            "Effect": "Allow",
            "Action": [
                "apigateway:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "EventBridgeAccess",
            "Effect": "Allow",
            "Action": [
                "events:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "SNSAccess",
            "Effect": "Allow",
            "Action": [
                "sns:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "SQSAccess",
            "Effect": "Allow",
            "Action": [
                "sqs:*"
            ],
            "Resource": "*"
        }
    ]
}
EOF

echo "📋 Creating new policy version with comprehensive permissions..."
aws iam create-policy-version \
    --policy-arn $POLICY_ARN \
    --policy-document file://streamlined-cicd-policy.json \
    --set-as-default

echo "✅ Policy updated successfully!"
echo ""
echo "🔍 Key permissions added:"
echo "- SSM: Full access for CDK bootstrap operations"
echo "- CloudFormation: Full access for stack management"
echo "- S3: Full access for CDK assets and deployment artifacts"
echo "- IAM: Extended permissions for role and policy management"
echo "- EC2: Full access for VPC and networking resources"
echo "- Lambda: Full access for serverless functions"
echo "- API Gateway: Full access for API management"
echo "- EventBridge: Full access for event-driven architecture"
echo "- SNS/SQS: Full access for messaging services"
echo "- Secrets Manager: Full access for secret management"
echo "- CloudWatch/Logs: Full access for monitoring and logging"
echo ""
echo "🧹 Cleaning up temporary files..."
rm -f streamlined-cicd-policy.json comprehensive-cicd-policy.json

echo "✅ CI/CD permissions updated! Your GitHub Actions should now have all necessary permissions for CDK deployment."
