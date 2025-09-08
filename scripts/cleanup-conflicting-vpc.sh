#!/bin/bash

# Script to clean up conflicting VPC resources
# Use with caution - this will delete the existing VPC and all its resources

set -e

echo "🔍 Checking for conflicting VPC resources..."

# Check if the conflicting VPC exists
CONFLICTING_VPC_ID="vpc-0eafc52ecbe64dd53"
VPC_EXISTS=$(aws ec2 describe-vpcs --vpc-ids "$CONFLICTING_VPC_ID" --query 'Vpcs[0].VpcId' --output text 2>/dev/null || echo "None")

if [ "$VPC_EXISTS" = "None" ]; then
    echo "✅ No conflicting VPC found. Safe to proceed with deployment."
    exit 0
fi

echo "⚠️  Found conflicting VPC: $CONFLICTING_VPC_ID"
echo "📋 VPC Details:"
aws ec2 describe-vpcs --vpc-ids "$CONFLICTING_VPC_ID" --query 'Vpcs[0].{VpcId:VpcId,CidrBlock:CidrBlock,State:State}' --output table

echo ""
echo "🔍 Checking for resources in this VPC..."

# Check for EC2 instances
INSTANCES=$(aws ec2 describe-instances --filters "Name=vpc-id,Values=$CONFLICTING_VPC_ID" --query 'Reservations[].Instances[?State.Name!=`terminated`].InstanceId' --output text)
if [ -n "$INSTANCES" ]; then
    echo "⚠️  Found EC2 instances: $INSTANCES"
else
    echo "✅ No EC2 instances found"
fi

# Check for RDS instances
RDS_INSTANCES=$(aws rds describe-db-instances --query "DBInstances[?DBSubnetGroup.VpcId=='$CONFLICTING_VPC_ID'].DBInstanceIdentifier" --output text)
if [ -n "$RDS_INSTANCES" ]; then
    echo "⚠️  Found RDS instances: $RDS_INSTANCES"
else
    echo "✅ No RDS instances found"
fi

# Check for Load Balancers
ALBS=$(aws elbv2 describe-load-balancers --query "LoadBalancers[?VpcId=='$CONFLICTING_VPC_ID'].LoadBalancerArn" --output text)
if [ -n "$ALBS" ]; then
    echo "⚠️  Found Application Load Balancers: $ALBS"
else
    echo "✅ No Application Load Balancers found"
fi

# Check for ECS clusters
ECS_CLUSTERS=$(aws ecs list-clusters --query "clusterArns[?contains(@, 'glamme') || contains(@, 'Glamme')]" --output text)
if [ -n "$ECS_CLUSTERS" ]; then
    echo "⚠️  Found ECS clusters: $ECS_CLUSTERS"
else
    echo "✅ No ECS clusters found"
fi

echo ""
echo "🤔 Options:"
echo "1. Delete the conflicting VPC (DANGEROUS - will delete all resources)"
echo "2. Keep the VPC and use a different CIDR (RECOMMENDED - already done)"
echo "3. Exit without changes"

read -p "Choose an option (1/2/3): " choice

case $choice in
    1)
        echo "⚠️  WARNING: This will delete the VPC and ALL its resources!"
        read -p "Are you sure? Type 'DELETE' to confirm: " confirm
        if [ "$confirm" = "DELETE" ]; then
            echo "🗑️  Deleting VPC $CONFLICTING_VPC_ID..."
            # Note: This is a simplified deletion - in practice, you'd need to delete all resources first
            echo "❌ Manual deletion required. Please delete all resources in the VPC first."
        else
            echo "❌ Deletion cancelled."
        fi
        ;;
    2)
        echo "✅ Good choice! The CDK has been updated to use CIDR 172.16.0.0/16"
        echo "✅ This avoids conflicts with the existing VPC"
        ;;
    3)
        echo "ℹ️  No changes made."
        ;;
    *)
        echo "❌ Invalid option."
        ;;
esac

echo ""
echo "🚀 Next steps:"
echo "1. Run your CDK deployment: cd cdk && cdk deploy GlammeVpcStack"
echo "2. The new VPC will use CIDR 172.16.0.0/16 to avoid conflicts"
echo "3. Subnets will be: 172.16.1.0/24, 172.16.2.0/24, etc."
