#!/bin/bash

# Optimize all Dockerfiles to use Alpine images for smaller size

set -e

echo "🐳 Optimizing Docker configurations..."

SERVICES=(
    "booking-service"
    "communication-service"
    "image-service"
    "matching-service"
    "reviews-service"
    "ride-service"
    "shopping-service"
    "social-service"
)

for service in "${SERVICES[@]}"; do
    echo "🔧 Optimizing Dockerfile.$service..."
    
    if [ -f "Dockerfile.$service" ]; then
        # Update to use Alpine images
        sed -i '' 's/amazoncorretto:17 AS builder/amazoncorretto:17-alpine AS builder/g' "Dockerfile.$service"
        sed -i '' 's/FROM amazoncorretto:17$/FROM amazoncorretto:17-alpine/g' "Dockerfile.$service"
        
        echo "✅ Optimized Dockerfile.$service"
    else
        echo "❌ Dockerfile.$service not found"
    fi
done

echo ""
echo "🎉 All Dockerfiles optimized!"
echo "📦 Benefits:"
echo "  • Smaller image sizes"
echo "  • Faster build times"
echo "  • Reduced security surface"
echo "  • Better for production deployment"
