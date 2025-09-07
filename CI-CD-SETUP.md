# 🚀 GlamMe CI/CD Pipeline Setup Guide

## 📋 Overview

This guide provides complete instructions for setting up automated CI/CD for the GlamMe platform using GitHub Actions and AWS. The pipeline automates the entire deployment process from code commit to production deployment.

## 🎯 Pipeline Features

### Automated Workflows
- **Build & Test**: Automated testing for all microservices
- **Docker Build**: Multi-stage Docker builds with caching
- **ECR Push**: Secure image push to Amazon ECR
- **Infrastructure Deployment**: CDK-based infrastructure as code
- **Service Deployment**: Automated ECS service updates
- **Health Checks**: Post-deployment verification

### Environment Support
- **Development**: Automatic deployment on feature branches
- **Staging**: Manual deployment for testing
- **Production**: Protected deployment with approval gates

---

## 🛠️ Prerequisites

### AWS Requirements
- AWS Account with appropriate permissions
- ECR repositories created (`glamme/*`)
- CDK bootstrap completed
- OIDC provider configured for GitHub Actions

### GitHub Requirements
- GitHub repository access
- Repository secrets configured
- Branch protection rules (optional)

### Local Requirements
- AWS CLI configured
- Docker installed
- GitHub CLI (optional)

---

## 🚀 Quick Setup

### Step 1: Run CI/CD Setup Script

```bash
# Make script executable
chmod +x setup-cicd.sh

# Run setup (creates IAM role and policies)
./setup-cicd.sh
```

This script will:
- ✅ Create IAM policy with necessary permissions
- ✅ Create IAM role for GitHub Actions
- ✅ Configure trust relationships
- ✅ Output the role ARN for GitHub secrets

### Step 2: Configure GitHub Secrets

1. Go to your GitHub repository
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Add the following secrets:

```bash
AWS_ROLE_ARN=arn:aws:iam::476114151082:role/GlamMe-CICD-Role
AWS_REGION=us-east-1
ACCOUNT_ID=476114151082
```

### Step 3: Push Code to GitHub

```bash
# Add, commit, and push all files
git add .
git commit -m "Add CI/CD pipeline configuration"
git push origin main
```

---

## 📁 Pipeline Structure

### Workflow Jobs

#### 1. Test & Build (`test`)
- ✅ Checkout code
- ✅ Set up JDK 17
- ✅ Cache Gradle dependencies
- ✅ Build common module
- ✅ Run tests for all services
- ✅ Build JAR files
- ✅ Upload build artifacts

#### 2. Build & Push Images (`build-and-push`)
- ✅ Download build artifacts
- ✅ Configure AWS credentials
- ✅ Login to ECR
- ✅ Build Docker images for all services
- ✅ Push images to ECR with multiple tags
- ✅ Verify ECR images

#### 3. Deploy Infrastructure (`deploy-infrastructure`)
- ✅ Configure AWS credentials
- ✅ Bootstrap CDK (if needed)
- ✅ Deploy CDK infrastructure
- ✅ Extract infrastructure outputs

#### 4. Deploy Services (`deploy-services`)
- ✅ Force new ECS service deployments
- ✅ Wait for services to stabilize
- ✅ Verify service health

#### 5. Test Deployment (`test-deployment`)
- ✅ Run deployment verification tests
- ✅ Health check all services
- ✅ Validate service connectivity

#### 6. Notification (`notify`)
- ✅ Report deployment status
- ✅ Notify stakeholders

---

## 🎛️ Pipeline Configuration

### Environment Variables

The pipeline uses the following environment variables:

```yaml
env:
  AWS_REGION: us-east-1
  ACCOUNT_ID: 476114151082
  ECR_REGISTRY: 476114151082.dkr.ecr.us-east-1.amazonaws.com
  ENVIRONMENT: ${{ github.event.inputs.environment || 'dev' }}
```

### Trigger Conditions

The pipeline triggers on:
- **Push to main/develop branches**
- **Pull requests to main**
- **Manual workflow dispatch**

### Branch Protection

For production deployments, consider:
- ✅ Required status checks
- ✅ Required reviews
- ✅ Branch protection rules
- ✅ Environment protection rules

---

## 🔧 Manual Pipeline Operations

### Trigger Manual Deployment

1. Go to GitHub repository **Actions** tab
2. Select **GlamMe CI/CD Pipeline** workflow
3. Click **Run workflow**
4. Select environment (dev/staging/prod)
5. Click **Run workflow**

### View Pipeline Status

```bash
# Check workflow runs
gh workflow list
gh workflow view deploy.yml

# View specific run
gh run list
gh run view <run-id>
```

### Debug Pipeline Issues

```bash
# View workflow logs
gh run view <run-id> --log

# Download artifacts
gh run download <run-id>
```

---

## 🔐 Security Configuration

### IAM Permissions

The CI/CD role has the following permissions:

#### ECR Access
- `ecr:GetAuthorizationToken`
- `ecr:BatchCheckLayerAvailability`
- `ecr:GetDownloadUrlForLayer`
- `ecr:PutImage`
- Repository management operations

#### ECS Deployment
- `ecs:UpdateService`
- `ecs:DescribeServices`
- `ecs:RegisterTaskDefinition`
- Service management operations

#### CloudFormation
- `cloudformation:*` (CDK operations)
- Stack management and updates

#### Additional Permissions
- S3 access for CDK assets
- CloudWatch logging
- Secrets Manager access

### OIDC Trust Policy

The IAM role uses OIDC federation:

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
                    "token.actions.githubusercontent.com:sub": "repo:OWNER/REPO:*"
                }
            }
        }
    ]
}
```

---

## 📊 Monitoring & Troubleshooting

### Pipeline Metrics

Monitor these key metrics:
- **Build Time**: Average build duration
- **Test Coverage**: Test pass/fail rates
- **Deployment Success**: Deployment success rates
- **Service Health**: Post-deployment health checks

### Common Issues

#### ECR Push Failures
```bash
# Check ECR permissions
aws ecr describe-repositories --repository-names glamme/auth-service

# Verify OIDC setup
aws iam get-role --role-name GlamMe-CICD-Role
```

#### CDK Deployment Issues
```bash
# Check CDK toolkit version
cdk --version

# Validate CDK app
cdk synth

# Check CloudFormation events
aws cloudformation describe-stack-events --stack-name GlammeVpcStack
```

#### ECS Service Issues
```bash
# Check service status
aws ecs describe-services --cluster glamme-cluster --services auth-service

# View service logs
aws logs tail /ecs/glamme-cluster/auth-service --follow
```

---

## 🚀 Advanced Configuration

### Multi-Environment Deployment

```yaml
# Add environment-specific configurations
jobs:
  deploy-staging:
    if: github.ref == 'refs/heads/develop'
    environment: staging
    # Staging-specific steps

  deploy-production:
    if: github.ref == 'refs/heads/main'
    environment: production
    # Production-specific steps
```

### Parallel Deployments

```yaml
# Deploy services in parallel
strategy:
  matrix:
    service: [auth-service, image-service, matching-service]
    # Parallel service deployments
```

### Rollback Strategy

```yaml
# Automatic rollback on failure
- name: Rollback on failure
  if: failure()
  run: |
    aws ecs update-service \
      --cluster glamme-cluster \
      --service ${{ matrix.service }} \
      --task-definition ${{ steps.previous-task.outputs.task-definition }}
```

---

## 📈 Best Practices

### Security
- ✅ Use OIDC for AWS access (no long-lived credentials)
- ✅ Least privilege IAM policies
- ✅ Regular secret rotation
- ✅ Code signing and verification

### Performance
- ✅ Docker layer caching
- ✅ Gradle dependency caching
- ✅ Parallel job execution
- ✅ Artifact reuse between jobs

### Reliability
- ✅ Automated testing
- ✅ Health checks
- ✅ Rollback procedures
- ✅ Monitoring and alerting

### Maintainability
- ✅ Clear documentation
- ✅ Modular workflow design
- ✅ Reusable actions and scripts
- ✅ Regular pipeline updates

---

## 🎯 Pipeline Status Dashboard

### Real-time Monitoring
- **GitHub Actions**: Pipeline status and logs
- **AWS CloudWatch**: Service metrics and logs
- **AWS X-Ray**: Distributed tracing
- **AWS Config**: Infrastructure compliance

### Alert Configuration
```yaml
# Slack notifications
- name: Notify Slack
  if: always()
  uses: 8398a7/action-slack@v3
  with:
    status: ${{ job.status }}
    webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

---

## 📞 Support & Resources

### Documentation
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [AWS CDK Documentation](https://docs.aws.amazon.com/cdk/)
- [Amazon ECR User Guide](https://docs.aws.amazon.com/ecr/)
- [Amazon ECS Developer Guide](https://docs.aws.amazon.com/ecs/)

### Useful Commands

#### GitHub CLI
```bash
# List workflows
gh workflow list

# View workflow runs
gh run list

# View specific run
gh run view <run-id>
```

#### AWS CLI
```bash
# Check ECR images
aws ecr describe-images --repository-name glamme/auth-service

# Check ECS services
aws ecs describe-services --cluster glamme-cluster

# Check CloudFormation stacks
aws cloudformation describe-stacks --stack-name GlammeVpcStack
```

---

## 🎉 Success Metrics

Track these KPIs for pipeline success:

- **Deployment Frequency**: Daily deployments to staging
- **Lead Time**: <15 minutes from commit to production
- **Change Failure Rate**: <5% deployment failures
- **Mean Time to Recovery**: <30 minutes for incidents

---

## 🚀 Next Steps

1. **Run the setup script**: `./setup-cicd.sh`
2. **Configure GitHub secrets**: Add AWS_ROLE_ARN
3. **Push to GitHub**: Trigger the first automated deployment
4. **Monitor the pipeline**: Watch the Actions tab
5. **Validate deployment**: Check service health endpoints

**Your GlamMe platform now has enterprise-grade CI/CD! 🎉**

**The pipeline will automatically handle all future deployments, testing, and infrastructure management.**
