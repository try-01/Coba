#!/bin/bash
set -e

cd "$(dirname "$0")"

echo "Samsung TV Remote - Build Script"
echo "================================"

# Check and create gradlew
if [ ! -f "./gradlew" ]; then
    echo "Gradle wrapper not found. Attempting to initialize..."
    
    if command -v gradle &> /dev/null; then
        gradle wrapper --gradle-version=8.7 --distribution-type=all
    else
        echo "ERROR: Gradle not installed!"
        echo ""
        echo "Install Gradle or use Android Studio:"
        echo "  - Install: https://gradle.org/install/"
        echo "  - Or open this project in Android Studio"
        exit 1
    fi
fi

chmod +x gradlew

echo "Building APK..."
./gradlew assembleDebug

echo "Build completed! APK: app/build/outputs/apk/debug/app-debug.apk"