# 🌟 GlamMe - Enterprise Beauty & Styling Platform

[![Build Status](https://github.com/ceesartech/Glam-Me/workflows/GlamMe%20CI/CD%20Pipeline/badge.svg)](https://github.com/ceesartech/Glam-Me/actions)
[![AWS](https://img.shields.io/badge/AWS-Cloud%20Native-orange.svg)](https://aws.amazon.com/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.java.net/projects/jdk/17/)

> **A comprehensive, cloud-native beauty and styling platform built with microservices architecture on AWS**

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Docker & Docker Compose
- AWS CLI configured
- Node.js 18+ (for CDK)

### Deploy to AWS
```bash
# 1. Setup AWS infrastructure
./scripts/setup-cicd.sh

# 2. Configure GitHub secrets (see docs/CI-CD-SETUP.md)

# 3. Deploy via GitHub Actions
git push origin main
```

### Local Development
```bash
# Build all services
./gradlew build

# Run validation
./scripts/validate-platform.sh

# Start services locally
docker-compose up
```

## 🏗️ Architecture

### Microservices
- **auth-service** - JWT/Cognito authentication
- **image-service** - AI image processing with Amazon Bedrock
- **matching-service** - Gale-Shapley algorithm with Elo scoring
- **booking-service** - Appointment scheduling with calendar sync
- **ride-service** - Multi-provider ride integration (Uber/Lyft)
- **shopping-service** - E-commerce with Stripe payments
- **communication-service** - Multi-channel messaging (SMS/Email/Push)
- **social-service** - Social features and content feeds
- **reviews-service** - Rating and review management

### AWS Services
- **ECS Fargate** - Container orchestration
- **Aurora PostgreSQL** - Primary database
- **ElastiCache Redis** - Caching and sessions
- **OpenSearch** - Search and analytics
- **S3** - Media storage
- **EventBridge** - Event-driven communication
- **Cognito** - User authentication
- **Bedrock** - AI image generation
- **CloudFront** - CDN

## 📁 Project Structure

```
GlamMe/
├── docs/                    # Documentation
│   ├── CI-CD-SETUP.md      # CI/CD configuration guide
│   ├── DEPLOYMENT-README.md # Deployment instructions
│   └── glamme-aws-architecture.md # Architecture overview
├── scripts/                 # Deployment & utility scripts
│   ├── setup-cicd.sh      # AWS IAM setup
│   ├── deploy.sh           # Manual deployment
│   ├── validate-platform.sh # Validation checks
│   └── test-deployment.sh  # Deployment testing
├── deployment/              # Infrastructure & configuration
│   ├── iac/                # IAM policies
│   └── config/             # Environment configs
├── cdk/                    # AWS CDK infrastructure
├── services/               # Microservices
│   ├── auth-service/
│   ├── booking-service/
│   ├── communication-service/
│   ├── image-service/
│   ├── matching-service/
│   ├── reviews-service/
│   ├── ride-service/
│   ├── shopping-service/
│   └── social-service/
├── common/                 # Shared libraries
├── .github/workflows/      # CI/CD pipelines
└── Dockerfile.*           # Service containers
```

## 🚀 Deployment

### Automated Deployment (Recommended)
The platform uses GitHub Actions for automated CI/CD:

1. **Setup AWS Infrastructure**
   ```bash
   ./scripts/setup-cicd.sh
   ```

2. **Configure GitHub Secrets**
   - `AWS_ROLE_ARN` - IAM role for GitHub Actions
   - `AWS_REGION` - Target AWS region
   - `ACCOUNT_ID` - AWS account ID

3. **Deploy**
   - Push to `main` branch for production
   - Push to `develop` for staging
   - Manual trigger available in GitHub Actions

### Manual Deployment
```bash
# Build services
./gradlew build

# Deploy infrastructure
cd cdk && cdk deploy

# Build and push Docker images
./scripts/deploy.sh

# Verify deployment
./scripts/test-deployment.sh
```

## 🔧 Development

### Running Tests
```bash
# Run all tests
./gradlew test

# Run specific service tests
./gradlew :auth-service:test
```

### Code Quality
```bash
# Validate platform
./scripts/validate-platform.sh

# Check dependencies
./scripts/check-dependencies.sh
```

### Building Docker Images
```bash
# Build all images
docker build -f Dockerfile.auth-service -t glamme/auth-service .
docker build -f Dockerfile.booking-service -t glamme/booking-service .
# ... repeat for all services
```

## 📋 Features

### Core Platform
- ✅ **Multi-tenant Architecture** - Scalable SaaS platform
- ✅ **Real-time Communication** - WebSocket support
- ✅ **AI-Powered Styling** - Bedrock image generation
- ✅ **Smart Matching** - Advanced algorithm for stylist-customer pairing
- ✅ **Integrated Payments** - Stripe integration across services
- ✅ **Multi-provider Rides** - Uber/Lyft integration
- ✅ **Social Platform** - Posts, feeds, and community features

### Technical Excellence
- ✅ **Microservices Architecture** - Independent, scalable services
- ✅ **Event-Driven Design** - EventBridge for loose coupling
- ✅ **CQRS Pattern** - Optimized read/write operations
- ✅ **Circuit Breakers** - Resilient external integrations
- ✅ **Distributed Caching** - Redis for performance
- ✅ **Comprehensive Monitoring** - CloudWatch integration

## 🛡️ Security

- **JWT Authentication** with AWS Cognito
- **IAM Roles** with least privilege access
- **Secrets Management** via AWS Secrets Manager
- **Network Security** with VPC and security groups
- **Data Encryption** at rest and in transit
- **WAF Protection** against common attacks

## 📊 Monitoring & Observability

- **CloudWatch Logs** - Centralized logging
- **CloudWatch Metrics** - Performance monitoring
- **X-Ray Tracing** - Distributed request tracing
- **Health Checks** - Service availability monitoring
- **Alerts** - Proactive issue detection

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Documentation**: [docs/](docs/)
- **Issues**: [GitHub Issues](https://github.com/ceesartech/Glam-Me/issues)
- **CI/CD**: [GitHub Actions](https://github.com/ceesartech/Glam-Me/actions)

## 🎯 Roadmap

- [ ] Mobile app development (React Native)
- [ ] Advanced AI features (style recommendations)
- [ ] International expansion
- [ ] Third-party integrations
- [ ] Performance optimizations

---

**Built with ❤️ by the GlamMe Team**