#!/bin/bash

# Comprehensive platform validation script for GlamMe
# Checks for common issues that could cause deployment failures

set -e

echo "🔍 GlamMe Platform Validation"
echo "============================="

# Color codes for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

CHECKS_PASSED=0
CHECKS_FAILED=0
WARNINGS=0

# Function to run a check
run_check() {
    local description="$1"
    local command="$2"
    local is_warning="${3:-false}"
    
    echo -n "🔍 $description... "
    
    if eval "$command" > /dev/null 2>&1; then
        if [ "$is_warning" = "true" ]; then
            echo -e "${YELLOW}WARNING${NC}"
            WARNINGS=$((WARNINGS + 1))
        else
            echo -e "${GREEN}PASS${NC}"
            CHECKS_PASSED=$((CHECKS_PASSED + 1))
        fi
    else
        if [ "$is_warning" = "true" ]; then
            echo -e "${YELLOW}WARNING${NC}"
            WARNINGS=$((WARNINGS + 1))
        else
            echo -e "${RED}FAIL${NC}"
            CHECKS_FAILED=$((CHECKS_FAILED + 1))
        fi
    fi
}

echo ""
echo "🏗️  Build System Checks"
echo "========================"

run_check "Gradle wrapper exists" "[ -f gradlew ]"
run_check "Gradle wrapper is executable" "[ -x gradlew ]"
run_check "settings.gradle exists" "[ -f settings.gradle ]"
run_check "Root build.gradle exists" "[ -f build.gradle ]"

echo ""
echo "📦 Service Structure Checks"
echo "============================"

SERVICES=("auth-service" "booking-service" "communication-service" "image-service" "matching-service" "reviews-service" "ride-service" "shopping-service" "social-service")

for service in "${SERVICES[@]}"; do
    run_check "$service directory exists" "[ -d $service ]"
    run_check "$service/build.gradle exists" "[ -f $service/build.gradle ]"
    run_check "$service/src/main/java exists" "[ -d $service/src/main/java ]"
    run_check "$service/src/main/resources exists" "[ -d $service/src/main/resources ]"
done

echo ""
echo "🐳 Docker Configuration Checks"
echo "==============================="

run_check "All root Dockerfiles exist" "ls Dockerfile.* > /dev/null 2>&1"
for service in "${SERVICES[@]}"; do
    run_check "Dockerfile.$service exists" "[ -f Dockerfile.$service ]"
done

echo ""
echo "🔧 Configuration Checks"
echo "========================"

run_check "CDK directory exists" "[ -d cdk ]"
run_check "CDK build.gradle exists" "[ -f cdk/build.gradle ]"
run_check "GitHub Actions workflow exists" "[ -f .github/workflows/deploy.yml ]"
run_check "IAM policy file exists" "[ -f iac/cicd-iam-policy.json ]"
run_check "IAM trust policy file exists" "[ -f iac/cicd-trust-policy.json ]"

echo ""
echo "📋 Application Configuration Checks"
echo "===================================="

for service in "${SERVICES[@]}"; do
    run_check "$service application.yaml exists" "[ -f $service/src/main/resources/application.yaml ]"
done

echo ""
echo "🏗️  Compilation Checks"
echo "======================="

run_check "All services compile" "./gradlew compileJava --no-daemon -q"
run_check "CDK compiles" "cd cdk && ./gradlew build --no-daemon -q"

echo ""
echo "🔍 Dependency Analysis"
echo "======================="

# Check for common missing dependencies
run_check "Lombok annotations present" "find . -name '*.java' -exec grep -l '@Data\|@RequiredArgsConstructor\|@Builder' {} \; | head -1 > /dev/null"
run_check "Spring Boot starters present" "find . -name 'build.gradle' -exec grep -l 'spring-boot-starter' {} \; | head -1 > /dev/null"
run_check "AWS SDK dependencies present" "find . -name 'build.gradle' -exec grep -l 'software.amazon.awssdk' {} \; | head -1 > /dev/null"

echo ""
echo "🚀 Deployment Readiness Checks"
echo "==============================="

run_check "AWS CLI available" "which aws"
run_check "Docker available" "which docker"
run_check "CDK CLI available" "which cdk"

echo ""
echo "📊 VALIDATION SUMMARY"
echo "====================="
echo -e "✅ Checks Passed: ${GREEN}$CHECKS_PASSED${NC}"
echo -e "❌ Checks Failed: ${RED}$CHECKS_FAILED${NC}"
echo -e "⚠️  Warnings: ${YELLOW}$WARNINGS${NC}"

if [ $CHECKS_FAILED -eq 0 ]; then
    echo ""
    echo -e "${GREEN}🎉 PLATFORM VALIDATION SUCCESSFUL!${NC}"
    echo "✅ All critical checks passed"
    echo "🚀 Platform is ready for deployment"
    
    if [ $WARNINGS -gt 0 ]; then
        echo -e "⚠️  ${WARNINGS} warnings detected - please review above"
    fi
    
    exit 0
else
    echo ""
    echo -e "${RED}❌ PLATFORM VALIDATION FAILED!${NC}"
    echo "🔧 Please fix the failed checks above before deployment"
    exit 1
fi
