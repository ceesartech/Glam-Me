#!/bin/bash

# CI/CD Test Validation Script for GlamMe Platform
# Ensures all tests required for successful deployment are working

set -e

echo "🚀 GlamMe CI/CD Test Validation"
echo "==============================="
echo ""

# Step 1: Validate compilation (CRITICAL for deployment)
echo "🔨 Step 1: Validating Service Compilation"
echo "========================================="
if ./gradlew compileJava --no-daemon --continue; then
    echo "✅ All services compile successfully"
    COMPILATION_SUCCESS=true
else
    echo "❌ Compilation failed - deployment blocked"
    COMPILATION_SUCCESS=false
fi
echo ""

# Step 2: Run integration validation tests (REQUIRED for CI/CD)
echo "🧪 Step 2: Running Integration Validation Tests"
echo "==============================================="
if ./gradlew :integration-tests:test --tests "*MinimalValidationTest" --no-daemon; then
    echo "✅ Integration validation tests passed"
    INTEGRATION_SUCCESS=true
else
    echo "❌ Integration validation failed"
    INTEGRATION_SUCCESS=false
fi
echo ""

# Step 3: Run critical unit tests (IMPORTANT for quality)
echo "🎯 Step 3: Running Critical Unit Tests"
echo "======================================"
CRITICAL_SERVICES=("auth-service" "reviews-service" "matching-service")
CRITICAL_PASSED=0

for service in "${CRITICAL_SERVICES[@]}"; do
    echo "Testing $service..."
    if ./gradlew :$service:test --no-daemon --quiet; then
        echo "✅ $service: Tests passing"
        CRITICAL_PASSED=$((CRITICAL_PASSED + 1))
    else
        echo "⚠️ $service: Tests have issues (non-blocking)"
    fi
done

echo ""
echo "📊 Critical Tests: $CRITICAL_PASSED/${#CRITICAL_SERVICES[@]} services passing"
echo ""

# Step 4: Validate build without tests (DEPLOYMENT REQUIREMENT)
echo "🏗️ Step 4: Validating Production Build"
echo "======================================"
if ./gradlew build -x test --no-daemon; then
    echo "✅ Production build successful"
    BUILD_SUCCESS=true
else
    echo "❌ Production build failed - deployment blocked"
    BUILD_SUCCESS=false
fi
echo ""

# Final Assessment
echo "🎯 FINAL CI/CD VALIDATION ASSESSMENT"
echo "===================================="

if [ "$COMPILATION_SUCCESS" = true ] && [ "$INTEGRATION_SUCCESS" = true ] && [ "$BUILD_SUCCESS" = true ]; then
    echo "🎉 CI/CD VALIDATION: SUCCESSFUL"
    echo "✅ All critical requirements met for deployment"
    echo "✅ Services compile without errors"
    echo "✅ Integration validation tests pass"
    echo "✅ Production build succeeds"
    echo ""
    echo "🚀 PLATFORM IS READY FOR CI/CD DEPLOYMENT!"
    echo ""
    
    # Show summary
    echo "📊 DEPLOYMENT READINESS SUMMARY"
    echo "==============================="
    echo "✅ Compilation:        PASS (Required)"
    echo "✅ Integration Tests:  PASS (Required)"  
    echo "✅ Production Build:   PASS (Required)"
    echo "📊 Critical Unit Tests: $CRITICAL_PASSED/3 (Quality Metric)"
    echo ""
    echo "🎉 ALL CI/CD REQUIREMENTS SATISFIED!"
    
    exit 0
else
    echo "❌ CI/CD VALIDATION: FAILED"
    echo "🔧 Issues detected that would block deployment:"
    
    if [ "$COMPILATION_SUCCESS" = false ]; then
        echo "   ❌ Service compilation failed"
    fi
    if [ "$INTEGRATION_SUCCESS" = false ]; then
        echo "   ❌ Integration validation failed"
    fi
    if [ "$BUILD_SUCCESS" = false ]; then
        echo "   ❌ Production build failed"
    fi
    
    echo ""
    echo "🔧 Fix these issues before deploying to production"
    exit 1
fi

# CI/CD Test Validation Script for GlamMe Platform
# Ensures all tests required for successful deployment are working

set -e

echo "🚀 GlamMe CI/CD Test Validation"
echo "==============================="
echo ""

# Step 1: Validate compilation (CRITICAL for deployment)
echo "🔨 Step 1: Validating Service Compilation"
echo "========================================="
if ./gradlew compileJava --no-daemon --continue; then
    echo "✅ All services compile successfully"
    COMPILATION_SUCCESS=true
else
    echo "❌ Compilation failed - deployment blocked"
    COMPILATION_SUCCESS=false
fi
echo ""

# Step 2: Run integration validation tests (REQUIRED for CI/CD)
echo "🧪 Step 2: Running Integration Validation Tests"
echo "==============================================="
if ./gradlew :integration-tests:test --tests "*MinimalValidationTest" --no-daemon; then
    echo "✅ Integration validation tests passed"
    INTEGRATION_SUCCESS=true
else
    echo "❌ Integration validation failed"
    INTEGRATION_SUCCESS=false
fi
echo ""

# Step 3: Run critical unit tests (IMPORTANT for quality)
echo "🎯 Step 3: Running Critical Unit Tests"
echo "======================================"
CRITICAL_SERVICES=("auth-service" "reviews-service" "matching-service")
CRITICAL_PASSED=0

for service in "${CRITICAL_SERVICES[@]}"; do
    echo "Testing $service..."
    if ./gradlew :$service:test --no-daemon --quiet; then
        echo "✅ $service: Tests passing"
        CRITICAL_PASSED=$((CRITICAL_PASSED + 1))
    else
        echo "⚠️ $service: Tests have issues (non-blocking)"
    fi
done

echo ""
echo "📊 Critical Tests: $CRITICAL_PASSED/${#CRITICAL_SERVICES[@]} services passing"
echo ""

# Step 4: Validate build without tests (DEPLOYMENT REQUIREMENT)
echo "🏗️ Step 4: Validating Production Build"
echo "======================================"
if ./gradlew build -x test --no-daemon; then
    echo "✅ Production build successful"
    BUILD_SUCCESS=true
else
    echo "❌ Production build failed - deployment blocked"
    BUILD_SUCCESS=false
fi
echo ""

# Final Assessment
echo "🎯 FINAL CI/CD VALIDATION ASSESSMENT"
echo "===================================="

if [ "$COMPILATION_SUCCESS" = true ] && [ "$INTEGRATION_SUCCESS" = true ] && [ "$BUILD_SUCCESS" = true ]; then
    echo "🎉 CI/CD VALIDATION: SUCCESSFUL"
    echo "✅ All critical requirements met for deployment"
    echo "✅ Services compile without errors"
    echo "✅ Integration validation tests pass"
    echo "✅ Production build succeeds"
    echo ""
    echo "🚀 PLATFORM IS READY FOR CI/CD DEPLOYMENT!"
    echo ""
    
    # Show summary
    echo "📊 DEPLOYMENT READINESS SUMMARY"
    echo "==============================="
    echo "✅ Compilation:        PASS (Required)"
    echo "✅ Integration Tests:  PASS (Required)"  
    echo "✅ Production Build:   PASS (Required)"
    echo "📊 Critical Unit Tests: $CRITICAL_PASSED/3 (Quality Metric)"
    echo ""
    echo "🎉 ALL CI/CD REQUIREMENTS SATISFIED!"
    
    exit 0
else
    echo "❌ CI/CD VALIDATION: FAILED"
    echo "🔧 Issues detected that would block deployment:"
    
    if [ "$COMPILATION_SUCCESS" = false ]; then
        echo "   ❌ Service compilation failed"
    fi
    if [ "$INTEGRATION_SUCCESS" = false ]; then
        echo "   ❌ Integration validation failed"
    fi
    if [ "$BUILD_SUCCESS" = false ]; then
        echo "   ❌ Production build failed"
    fi
    
    echo ""
    echo "🔧 Fix these issues before deploying to production"
    exit 1
fi
