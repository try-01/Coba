#!/usr/bin/env bash
set -euo pipefail

# ─────────────────────────────────────────────────────────────
# Samsung TV Remote — build script
# ─────────────────────────────────────────────────────────────
# Usage:  ./build.sh [assembleDebug|assembleRelease|installDebug]
# Default: assembleDebug
# ─────────────────────────────────────────────────────────────

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

ACTION="${1:-assembleDebug}"

# Ensure Gradle wrapper exists
if [ ! -f gradlew ]; then
    echo ":: Generating Gradle wrapper…"
    if command -v gradle &>/dev/null; then
        gradle wrapper --gradle-version 8.9
    else
        echo "ERROR: 'gradle' not found and no gradlew present."
        echo "       Install Gradle or commit the wrapper files."
        exit 1
    fi
fi

chmod +x gradlew

# Auto-create local.properties if ANDROID_HOME is set
if [ -n "${ANDROID_HOME:-}" ] && [ ! -f local.properties ]; then
    echo "sdk.dir=$ANDROID_HOME" > local.properties
    echo ":: Created local.properties from ANDROID_HOME"
elif [ -n "${ANDROID_SDK_ROOT:-}" ] && [ ! -f local.properties ]; then
    echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties
    echo ":: Created local.properties from ANDROID_SDK_ROOT"
fi

echo ":: Running ./gradlew $ACTION …"
./gradlew "$ACTION" --no-daemon --stacktrace
