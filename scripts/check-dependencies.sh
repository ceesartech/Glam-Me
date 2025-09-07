#!/bin/bash

# Comprehensive dependency and configuration check for all GlamMe services
# This script ensures all services have required classes and dependencies

set -e

echo "🔍 GlamMe Services Dependency Check"
echo "=================================="

SERVICES=(
    "auth-service"
    "booking-service"
    "communication-service"
    "image-service"
    "matching-service"
    "reviews-service"
    "ride-service"
    "shopping-service"
    "social-service"
    "common"
)

TOTAL_SERVICES=${#SERVICES[@]}
PASSED=0
FAILED=0

echo ""
echo "📋 Testing ${TOTAL_SERVICES} services..."
echo ""

for service in "${SERVICES[@]}"; do
    echo "=== Testing $service ==="
    
    # Check if service directory exists
    if [ ! -d "$service" ]; then
        echo "❌ Service directory not found: $service"
        FAILED=$((FAILED + 1))
        continue
    fi
    
    # Check if build.gradle exists
    if [ ! -f "$service/build.gradle" ]; then
        echo "❌ build.gradle not found for $service"
        FAILED=$((FAILED + 1))
        continue
    fi
    
    # Test compilation
    echo "🔨 Compiling $service..."
    if ./gradlew :$service:compileJava --no-daemon -q; then
        echo "✅ $service compilation passed"
        PASSED=$((PASSED + 1))
    else
        echo "❌ $service compilation failed"
        FAILED=$((FAILED + 1))
        
        # Show detailed error for failed compilation
        echo "📝 Error details for $service:"
        ./gradlew :$service:compileJava --no-daemon 2>&1 | tail -20
    fi
    
    echo ""
done

echo ""
echo "📊 SUMMARY"
echo "=========="
echo "✅ Passed: $PASSED/$TOTAL_SERVICES"
echo "❌ Failed: $FAILED/$TOTAL_SERVICES"

if [ $FAILED -eq 0 ]; then
    echo ""
    echo "🎉 ALL SERVICES COMPILE SUCCESSFULLY!"
    echo "✅ No missing dependencies or configuration classes detected"
    echo "🚀 Platform is ready for deployment"
    exit 0
else
    echo ""
    echo "⚠️  SOME SERVICES HAVE COMPILATION ISSUES"
    echo "🔧 Please fix the issues above before deployment"
    exit 1
fi
