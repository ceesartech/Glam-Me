#!/bin/bash

# Final cleanup script for GlamMe platform
# Removes any remaining temporary files and ensures clean deployment

set -e

echo "🧹 Final Cleanup for GlamMe Platform"
echo "===================================="

# Clean build artifacts
echo "🔄 Cleaning build artifacts..."
./gradlew clean --no-daemon

# Remove any temporary files
echo "🗑️  Removing temporary files..."
find . -name "*.tmp" -delete 2>/dev/null || true
find . -name "*.log" -delete 2>/dev/null || true
find . -name ".DS_Store" -delete 2>/dev/null || true
find . -name "Thumbs.db" -delete 2>/dev/null || true

# Clean CDK output
echo "🏗️  Cleaning CDK outputs..."
rm -rf cdk/cdk.out/* 2>/dev/null || true

# Ensure all scripts are executable
echo "🔧 Making scripts executable..."
chmod +x scripts/*.sh

# Validate platform one final time
echo "✅ Running final validation..."
./scripts/validate-platform.sh

echo ""
echo "🎉 CLEANUP COMPLETE!"
echo "==================="
echo ""
echo "📊 Platform Status:"
echo "  ✅ All build artifacts cleaned"
echo "  ✅ Temporary files removed"
echo "  ✅ Scripts executable"
echo "  ✅ Platform validated"
echo ""
echo "🚀 Your GlamMe platform is now optimized and ready for deployment!"
