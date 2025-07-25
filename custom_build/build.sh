#!/bin/bash

# Load custom build properties
source ./config/custom.properties

# Check if all required variables are set in config/custom.properties
requiredVars=(
  "applicationId"
  "versionCode"
  "versionName"
  "appName"
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

# copy icons
rm -rf ./../app/src/main/res/mipmap*
cp -r ./config/android/res ./../app/src/main
cp ./config/androidtv/res/drawable-xhdpi/* ./../app/src/main/res/drawable-xhdpi/

cd ..
./gradlew assembleDefaultRelease bundleDefaultRelease \
  -PapplicationId="$applicationId" \
  -PversionCode=$versionCode \
  -PversionName="$versionName" \
  -PappName="$appName" \
  -PdefaultPropertyId="$defaultPropertyId" \
  -Pandroid.injected.signing.store.file=$keystoreFile \
  -Pandroid.injected.signing.store.password=$KEYSTORE_PASSWORD \
  -Pandroid.injected.signing.key.alias=$KEYSTORE_ALIAS \
  -Pandroid.injected.signing.key.password=$KEYSTORE_PASSWORD

# Make a copy of the generated APK and AAB files
mkdir -p ./custom_build/build_output
cp ./app/build/outputs/apk/default/release/app-default-release.apk ./custom_build/build_output/
cp ./app/build/outputs/bundle/defaultRelease/app-default-release.aab ./custom_build/build_output/
echo "Build completed successfully."
