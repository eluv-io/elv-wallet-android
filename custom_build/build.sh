#!/bin/bash

# Stop script on error
set -e

# capture verifyOnly flag
verifyOnly=0
if [ "$1" == "-v" ]; then
  verifyOnly=1
fi

# Default JAVA_HOME if not set
if [ -z "$JAVA_HOME" ]; then
  export JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home
fi

# Load custom build properties
source ./config/custom.properties

# Check if all required variables are set in config/custom.properties
requiredVars=(
  "APPLICATION_ID"
  "VERSION_CODE"
  "VERSION_NAME"
  "APP_NAME"
  "KEYSTORE_PASSWORD"
  "KEYSTORE_ALIAS")

missingVars=()
for var in "${requiredVars[@]}"; do
  if [ -z "${!var}" ]; then
    missingVars+=("$var")
  fi
done
if [ ${#missingVars[@]} -ne 0 ]; then
  echo "Missing required variables in config/custom.properties: ${missingVars[*]}"
  exit 1
fi

if ! [[ "$APPLICATION_ID" =~ ^([a-zA-Z_][a-zA-Z0-9_]*)(\.[a-zA-Z_][a-zA-Z0-9_]*)*$ ]]; then
  echo "Invalid APPLICATION_ID package name: $APPLICATION_ID. Must be a valid Java package name (alphanumeric, underscores, and dots)."
  exit 1
fi

# verify config/android and config/androidtv directories exist
if [[ ! -d "./config/android"  || ! -d "./config/androidtv" ]]; then
  echo "Missing icon resource directories (config/android or config/androidtv)."
  exit 1
fi

# find a .keystore or .jks file
keystoreFile=$(find ./config -type f \( -name "*.keystore" -o -name "*.jks" \) | head -n 1)
if [ -z "$keystoreFile" ]; then
  echo "No keystore file found in config directory."
  exit 1
fi
keystoreFile=$(realpath "$keystoreFile")

if [ $verifyOnly -eq 1 ]; then
  echo "Config verification successful."
  exit 0
fi

# copy icons
rm -rf ./../app/src/main/res/mipmap*
cp -r ./config/android/res ./../app/src/main
cp ./config/androidtv/res/drawable-xhdpi/* ./../app/src/main/res/drawable-xhdpi/

cd ..
./gradlew assembleDefaultDebug assembleDefaultRelease bundleDefaultRelease \
  -PapplicationId="$APPLICATION_ID" \
  -PversionCode=$VERSION_CODE \
  -PversionName="$VERSION_NAME" \
  -PappName="$APP_NAME" \
  -PdefaultPropertyId="$DEFAULT_PROPERTY_ID" \
  -PdefaultToStaging=$DEFAULT_TO_STAGING \
  -Pandroid.injected.signing.store.file=$keystoreFile \
  -Pandroid.injected.signing.store.password=$KEYSTORE_PASSWORD \
  -Pandroid.injected.signing.key.alias=$KEYSTORE_ALIAS \
  -Pandroid.injected.signing.key.password=$KEYSTORE_PASSWORD

# Make a copy of the generated APK and AAB files
mkdir -p ./custom_build/build_output
cp ./app/build/outputs/apk/default/release/app-default-release.apk ./custom_build/build_output/
cp ./app/build/outputs/apk/default/debug/app-default-debug.apk ./custom_build/build_output/
cp ./app/build/outputs/bundle/defaultRelease/app-default-release.aab ./custom_build/build_output/
echo "Build completed successfully. APK and AAB at: $(pwd)/custom_build/build_output"
