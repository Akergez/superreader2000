#!/bin/bash

# PDF Reader KMP - Quick Start Script
# This script helps set up and build the multiplatform PDF reader

set -e

echo "🚀 PDF Reader KMP - Quick Start"
echo "================================"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check Java version
echo "📋 Checking Java version..."
java_version=$(java -version 2>&1 | grep -oP 'version "\K[^"]+')
echo "   Java version: $java_version"

# Check for Android SDK
if [ -z "$ANDROID_HOME" ]; then
    echo "${YELLOW}⚠️  ANDROID_HOME not set${NC}"
    echo "   Set it to build Android app, e.g.:"
    echo "   export ANDROID_HOME=~/Library/Android/sdk"
else
    echo "${GREEN}✅ ANDROID_HOME set: $ANDROID_HOME${NC}"
fi

echo ""
echo "📦 Building Shared Module..."
./gradlew shared:build || exit 1
echo "${GREEN}✅ Shared module built${NC}"

echo ""
echo "📱 Building Android App..."
./gradlew bottomapp:build || exit 1
echo "${GREEN}✅ Android app built${NC}"

echo ""
echo "🖥️  Building Desktop App..."
./gradlew desktopApp:build || exit 1
echo "${GREEN}✅ Desktop app built${NC}"

echo ""
echo "================================"
echo "${GREEN}✅ Build Complete!${NC}"
echo ""
echo "🎯 Next Steps:"
echo ""
echo "Run Android app:"
echo "  ./gradlew bottomapp:installDebug"
echo "  ./gradlew bottomapp:run"
echo ""
echo "Run Desktop app:"
echo "  ./gradlew desktopApp:run"
echo ""
echo "Package Desktop for distribution:"
echo "  ./gradlew desktopApp:packageMsi      # Windows"
echo "  ./gradlew desktopApp:packageDeb      # Linux"
echo "  ./gradlew desktopApp:packageDmg      # macOS"
echo ""
echo "Run tests:"
echo "  ./gradlew shared:test"
echo "  ./gradlew bottomapp:test"
echo ""
echo "For more info, see:"
echo "  - MULTIPLATFORM_MIGRATION.md"
echo "  - IMPLEMENTATION_GUIDE.md"

