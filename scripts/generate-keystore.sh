#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
mkdir -p keystore

STORE_PW="${STORE_PASSWORD:-${KEYSTORE_PASSWORD:-changeit}}"
KEY_PW="${KEY_PASSWORD:-${KEYSTORE_KEY_PASSWORD:-${STORE_PW}}}"

keytool -genkeypair -v \
    -keystore "keystore/yslam-release.jks" \
    -alias yslams \
    -keyalg RSA \
    -keysize 4096 \
    -validity 10000 \
    -storepass "$STORE_PW" \
    -keypass "$KEY_PW" \
    -dname "CN=YSLAM, OU=Mobile, O=YSLAM, C=US"

chmod 600 "keystore/yslam-release.jks"
echo "Keystore created at keystore/yslam-release.jks"
