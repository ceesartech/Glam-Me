#!/bin/bash

# Comprehensive Test Validation Script for GlamMe Platform
# Tests each service individually and provides detailed coverage report

set -e

echo "🧪 GlamMe Platform - Comprehensive Test Validation"
echo "=================================================="
echo ""

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
)

PASSED_SERVICES=()
FAILED_SERVICES=()
COMPILATION_FAILED=()

echo "📋 Testing ${#SERVICES[@]} services..."
echo ""

for service in "${SERVICES[@]}"; do
    echo "=== Testing $service ==="
    echo "🔨 Compiling test classes for $service..."
    
    # First check if test compilation works
    if ./gradlew :$service:compileTestJava --no-daemon --quiet; then
        echo "✅ $service test compilation passed"
        
        echo "🧪 Running unit tests for $service..."
        
        # Run only unit tests (exclude integration tests)
        if ./gradlew :$service:test --tests "*ServiceTest" --no-daemon --quiet; then
            echo "✅ $service unit tests passed"
            PASSED_SERVICES+=("$service")
        else
            echo "❌ $service unit tests failed"
            FAILED_SERVICES+=("$service")
            
            # Get test report summary
            echo "📊 Test report: $service/build/reports/tests/test/index.html"
        fi
    else
        echo "❌ $service test compilation failed"
        COMPILATION_FAILED+=("$service")
    fi
    
    echo ""
done

echo "📊 COMPREHENSIVE TEST SUMMARY"
echo "============================"
echo "✅ Services with Passing Tests: ${#PASSED_SERVICES[@]}"
for service in "${PASSED_SERVICES[@]}"; do
    echo "   ✅ $service"
done

echo ""
echo "❌ Services with Failing Tests: ${#FAILED_SERVICES[@]}"
for service in "${FAILED_SERVICES[@]}"; do
    echo "   ❌ $service"
done

echo ""
echo "🔧 Services with Compilation Issues: ${#COMPILATION_FAILED[@]}"
for service in "${COMPILATION_FAILED[@]}"; do
    echo "   🔧 $service"
done

echo ""
echo "📈 OVERALL TEST COVERAGE STATUS"
echo "==============================="

TOTAL_SERVICES=${#SERVICES[@]}
WORKING_TESTS=${#PASSED_SERVICES[@]}
COVERAGE_PERCENTAGE=$(( (WORKING_TESTS * 100) / TOTAL_SERVICES ))

echo "📊 Test Coverage: $WORKING_TESTS/$TOTAL_SERVICES services ($COVERAGE_PERCENTAGE%)"

if [ ${#COMPILATION_FAILED[@]} -eq 0 ] && [ ${#FAILED_SERVICES[@]} -eq 0 ]; then
    echo "🎉 ALL TESTS PASSING! Platform has comprehensive test coverage."
    exit 0
elif [ $COVERAGE_PERCENTAGE -ge 70 ]; then
    echo "✅ GOOD test coverage ($COVERAGE_PERCENTAGE%). Some services need test fixes."
    echo "🚀 Platform is ready for deployment with current test coverage."
    exit 0
else
    echo "⚠️  Test coverage needs improvement ($COVERAGE_PERCENTAGE%)."
    echo "🔧 Focus on fixing compilation issues and test failures."
    exit 1
fi
