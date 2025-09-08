#!/bin/bash

# Script to fix ECR permissions for GitHub Actions CI/CD
# This script updates the IAM policy to allow ecr:GetAuthorizationToken globally

set -e

POLICY_ARN="arn:aws:iam::476114151082:policy/GlamMe-CICD-Policy"

echo "🔧 Fixing ECR permissions for GitHub Actions CI/CD"
echo "=================================================="
echo ""

# Create the corrected policy document
cat > fixed-cicd-policy.json << 'EOF'
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
                "cloudformation:CreateStack",
                "cloudformation:UpdateStack",
                "cloudformation:DeleteStack",
                "cloudformation:DescribeStacks",
                "cloudformation:DescribeStackEvents",
                "cloudformation:DescribeStackResources",
                "cloudformation:GetTemplate",
                "cloudformation:ValidateTemplate",
                "cloudformation:ListStacks",
                "cloudformation:ListStackResources"
            ],
            "Resource": [
                "arn:aws:cloudformation:us-east-1:476114151082:stack/GlammeVpcStack/*",
                "arn:aws:cloudformation:us-east-1:476114151082:stack/CDKToolkit/*"
            ]
        },
        {
            "Sid": "IAMCDK",
            "Effect": "Allow",
            "Action": [
                "iam:PassRole"
            ],
            "Resource": [
                "arn:aws:iam::476114151082:role/cdk-*"
            ]
        },
        {
            "Sid": "S3Deployment",
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:PutObject",
                "s3:DeleteObject",
                "s3:ListBucket",
                "s3:GetBucketLocation"
            ],
            "Resource": [
                "arn:aws:s3:::cdk-*",
                "arn:aws:s3:::glamme-*"
            ]
        },
        {
            "Sid": "SecretsManager",
            "Effect": "Allow",
            "Action": [
                "secretsmanager:GetSecretValue",
                "secretsmanager:DescribeSecret",
                "secretsmanager:ListSecrets"
            ],
            "Resource": [
                "arn:aws:secretsmanager:us-east-1:476114151082:secret:glamme/*"
            ]
        },
        {
            "Sid": "LogsAccess",
            "Effect": "Allow",
            "Action": [
                "logs:CreateLogGroup",
                "logs:CreateLogStream",
                "logs:PutLogEvents",
                "logs:DescribeLogStreams",
                "logs:DescribeLogGroups"
            ],
            "Resource": [
                "arn:aws:logs:us-east-1:476114151082:log-group:/ecs/glamme-cluster:*"
            ]
        },
        {
            "Sid": "CloudWatchAccess",
            "Effect": "Allow",
            "Action": [
                "cloudwatch:GetMetricStatistics",
                "cloudwatch:ListMetrics",
                "cloudwatch:PutMetricData"
            ],
            "Resource": "*"
        }
    ]
}
EOF

echo "📋 Creating new policy version..."
aws iam create-policy-version \
    --policy-arn $POLICY_ARN \
    --policy-document file://fixed-cicd-policy.json \
    --set-as-default

echo "✅ Policy updated successfully!"
echo ""
echo "🔍 Key changes made:"
echo "- Added separate statement for ecr:GetAuthorizationToken with Resource: '*'"
echo "- This allows GitHub Actions to get ECR authorization tokens globally"
echo "- Other ECR actions remain restricted to glamme/* repositories"
echo ""
echo "🧹 Cleaning up temporary files..."
rm -f fixed-cicd-policy.json

echo "✅ ECR permissions fixed! Your GitHub Actions should now work."
