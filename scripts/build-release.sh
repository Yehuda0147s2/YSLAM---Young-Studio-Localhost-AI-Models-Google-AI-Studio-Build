#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

VERSION_NAME="${VERSION_NAME:-1.0.0}"
VERSION_CODE="${VERSION_CODE:-$(date +%y%m%d%H)}"
KEYSTORE_PATH="${KEYSTORE_PATH:-${PWD}/keystore/yslam-release.jks}"

if [ -z "${STORE_PASSWORD:-}" ] || [ -z "${KEY_PASSWORD:-}" ] || [ ! -f "${KEYSTORE_PATH}" ]; then
    echo "Signing not configured. Run:"
    echo "  bash scripts/generate-keystore.sh"
    echo "  export KEYSTORE_PATH=$(pwd)/keystore/yslam-release.jks"
    echo "  export STORE_PASSWORD=..."
    echo "  export KEY_PASSWORD=..."
    exit 1
fi

echo "Building release APK (v${VERSION_NAME}, code ${VERSION_CODE})..."
./gradlew clean :app:assembleRelease \
    -PversionCodeOverride="$VERSION_CODE" \
    -PversionNameOverride="$VERSION_NAME"

APK="app/build/outputs/apk/release/app-release.apk"
if [ ! -f "$APK" ]; then
    echo "Release APK was not produced: $APK"
    exit 1
fi

cp "$APK" "./yslam-${VERSION_NAME}-release.apk"
echo "Release APK produced at ./yslam-${VERSION_NAME}-release.apk"
