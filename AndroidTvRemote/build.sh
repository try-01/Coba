#!/bin/bash
set -e

cd "$(dirname "$0")"

echo "Samsung TV Remote - Build Script"
echo "================================"

if [ -f "./gradlew" ]; then
    chmod +x gradlew
    echo "Building with Gradle wrapper..."
    ./gradlew assembleDebug
else
    echo "ERROR: gradlew not found!"
    echo ""
    echo "To build this Android project:"
    echo "  1. Initialize Gradle wrapper: gradle wrapper --gradle-version=8.7"
    echo "  2. Or use Android Studio to open and build the project"
    exit 1
fi

if [ $? -eq 0 ]; then
    echo "Build successful! APK location: app/build/outputs/apk/debug/app-debug.apk"
else
    echo "Build failed!"
    exit 1
fi