#!/bin/bash

# Stop script on error
set -e

usage() {
  cat <<'USAGE'
Usage: ./build.sh [options]

  (no options)     Build debug + release APKs and a release AAB into build_output/
  -i, --install    Build only the debug APK, then install and launch it on the
                   connected device. Much faster than a full build - use this while
                   iterating on your branding.
  -v, --verify     Check the configuration and exit without building.
  -h, --help       Show this message.
USAGE
}

verifyOnly=0
installAfterBuild=0
while [ $# -gt 0 ]; do
  case "$1" in
    -v | --verify) verifyOnly=1 ;;
    -i | --install) installAfterBuild=1 ;;
    -h | --help) usage; exit 0 ;;
    *) echo "Unknown option: $1"; echo; usage; exit 1 ;;
  esac
  shift
done

# Pick a JDK if the environment hasn't. Android Studio's bundled JBR is the convenient default
# for people who haven't set up Java separately, but Gradle only supports up to JDK 21 here and
# recent Android Studio ships a newer one - using it fails with a bare version number, which is
# impossible to diagnose. So check the version before trusting it, and otherwise leave JAVA_HOME
# alone and let Gradle find a JDK itself.
maxJdk=21
if [ -z "$JAVA_HOME" ]; then
  androidStudioJbr="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  if [ -x "$androidStudioJbr/bin/java" ]; then
    jbrMajor=$("$androidStudioJbr/bin/java" -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')
    if [ -n "$jbrMajor" ] && [ "$jbrMajor" -le "$maxJdk" ] 2>/dev/null; then
      export JAVA_HOME="$androidStudioJbr"
    fi
  fi
fi

# Whatever we ended up with, fail early and clearly if it's too new.
javaMajor=$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')
if [ -n "$javaMajor" ] && [ "$javaMajor" -gt "$maxJdk" ] 2>/dev/null; then
  echo "Java $javaMajor is too new for this project's Gradle - JDK $maxJdk or older is required."
  echo "Point JAVA_HOME at a supported JDK, for example:"
  echo "  export JAVA_HOME=\$(/usr/libexec/java_home -v $maxJdk)"
  exit 1
fi

# Load custom build properties
source ./config/custom.properties

# Load signing config, if there is any. Kept separate from custom.properties because that file
# is meant to be committed and this one never is - see config/signing.properties.template.
# With no signing config at all we fall back to the Android debug keystore further down.
if [ -f ./config/signing.properties ]; then
  source ./config/signing.properties
fi

# Check if all required variables are set in config/custom.properties
requiredVars=(
  "APPLICATION_ID"
  "VERSION_CODE"
  "VERSION_NAME"
  "APP_NAME")

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

# Locate the keystore. An explicit KEYSTORE_FILE is preferred - pointing it outside the repo
# means the key can't be committed even by accident. Otherwise fall back to whatever key is
# sitting in config/.
if [ -n "$KEYSTORE_FILE" ]; then
  keystoreFile="${KEYSTORE_FILE/#\~/$HOME}"
  if [ ! -f "$keystoreFile" ]; then
    echo "KEYSTORE_FILE is set to '$KEYSTORE_FILE' but no file exists there."
    exit 1
  fi
else
  keystoreFile=$(find ./config -type f \( -name "*.keystore" -o -name "*.jks" \) | head -n 1)
  if [ -z "$keystoreFile" ]; then
    # No signing config at all: fall back to the Android debug keystore so you can get a
    # runnable APK without setting anything up. Guarded by a confirmation below, because these
    # builds can never be published.
    debugKeystore="$HOME/.android/debug.keystore"
    if [ ! -f "$debugKeystore" ]; then
      echo "No keystore found, and no Android debug keystore at $debugKeystore."
      echo "Set KEYSTORE_FILE in config/signing.properties, or place a .keystore/.jks file"
      echo "in the config folder. See docs/Configuration.md#keystore-and-signing."
      exit 1
    fi
    usingDebugKeystore=1
    keystoreFile="$debugKeystore"
    KEYSTORE_ALIAS="androiddebugkey"
    KEYSTORE_PASSWORD="android"
    KEYSTORE_ALIAS_PASSWORD="android"
  fi
fi
keystoreFile=$(realpath "$keystoreFile")

if [ -z "$KEYSTORE_ALIAS" ]; then
  echo "KEYSTORE_ALIAS is not set in config/signing.properties."
  exit 1
fi

# Refuse to build if signing material is tracked by git. .gitignore only protects files that
# were never committed - this catches a key that was added before the ignore rules existed,
# which is exactly how signing keys end up published. Every key in config/ is checked, not just
# the one being used: a stray tracked key is just as published as the active one.
signingFiles=("$keystoreFile" "./config/signing.properties")
while IFS= read -r f; do signingFiles+=("$f"); done < <(
  find ./config -type f \( -name "*.keystore" -o -name "*.jks" \) 2>/dev/null
)
for tracked in "${signingFiles[@]}"; do
  if git ls-files --error-unmatch "$tracked" >/dev/null 2>&1; then
    echo "REFUSING TO BUILD: '$tracked' is tracked by git."
    echo "Your signing key would be published on your next push. Remove it from git with:"
    echo "  git rm --cached \"$tracked\""
    echo "If it has already been pushed, treat the key as compromised and rotate it."
    exit 1
  fi
done

if [ "$usingDebugKeystore" == "1" ]; then
  # Colour only when attached to a terminal, so piped/redirected output stays readable.
  if [ -t 1 ]; then red='\033[1;31m'; yellow='\033[1;33m'; reset='\033[0m'; else red=''; yellow=''; reset=''; fi
  printf "%b" "$red"
  echo "################################################################################"
  echo "#                                                                              #"
  echo "#   WARNING: SIGNING WITH THE ANDROID DEBUG KEYSTORE                           #"
  echo "#                                                                              #"
  echo "################################################################################"
  printf "%b" "$reset$yellow"
  echo
  echo "  No signing config was found, so this build will be signed with the debug key at"
  echo "  $debugKeystore."
  echo
  echo "  These builds CANNOT be published to the Google Play Store, and cannot be"
  echo "  updated later by a properly signed build - Android treats a change of signing"
  echo "  key as a different app. The debug key's password is public knowledge, so anyone"
  echo "  can produce an APK that looks identical to yours."
  echo
  echo "  Use this for local testing and sideloading only. To make a publishable build,"
  echo "  see docs/Configuration.md#keystore-and-signing."
  printf "%b" "$reset"
  echo

  if [ $verifyOnly -eq 0 ]; then
    if [ "$ALLOW_DEBUG_SIGNING" == "1" ]; then
      echo "ALLOW_DEBUG_SIGNING=1 set, continuing without asking."
    elif [ ! -t 0 ]; then
      # Nothing to read a confirmation from - fail closed rather than silently debug-sign.
      echo "Refusing to debug-sign non-interactively. Re-run in a terminal, or set"
      echo "ALLOW_DEBUG_SIGNING=1 if you really mean it."
      exit 1
    else
      read -r -p "  Continue with a debug-signed build? [y/N] " confirm
      case "$confirm" in
        [yY] | [yY][eE][sS]) echo ;;
        *) echo "Aborted."; exit 1 ;;
      esac
    fi
  fi
fi

if [ $verifyOnly -eq 1 ]; then
  echo "Config verification successful."
  exit 0
fi

# Prompt for anything left blank, so passwords can stay off disk entirely.
if [ -z "$KEYSTORE_PASSWORD" ]; then
  read -r -s -p "Keystore password: " KEYSTORE_PASSWORD
  echo
  if [ -z "$KEYSTORE_PASSWORD" ]; then
    echo "A keystore password is required."
    exit 1
  fi
fi
# Most keystores use the same password for the key; only ask if they differ.
if [ -z "$KEYSTORE_ALIAS_PASSWORD" ]; then
  KEYSTORE_ALIAS_PASSWORD="$KEYSTORE_PASSWORD"
fi

# copy icons
rm -rf ./../tv/src/main/res/mipmap*
cp -r ./config/android/res ./../tv/src/main
cp ./config/androidtv/res/drawable-xhdpi/* ./../tv/src/main/res/drawable-xhdpi/

cd ..
# :tv:-scoped - unqualified task names would also build :mobile, which isn't customizable.
# --install skips the release APK and the bundle; neither is useful for a test install, and
# R8 accounts for most of the build time.
gradleTasks=(:tv:assembleDebug)
if [ $installAfterBuild -eq 0 ]; then
  gradleTasks+=(:tv:assembleRelease :tv:bundleRelease)
fi
./gradlew "${gradleTasks[@]}" \
  -PapplicationId="$APPLICATION_ID" \
  -PversionCode=$VERSION_CODE \
  -PversionName="$VERSION_NAME" \
  -PappName="$APP_NAME" \
  -PdefaultPropertyId="$DEFAULT_PROPERTY_ID" \
  -PdefaultToStaging=$DEFAULT_TO_STAGING \
  -Pandroid.injected.signing.store.file="$keystoreFile" \
  -Pandroid.injected.signing.store.password="$KEYSTORE_PASSWORD" \
  -Pandroid.injected.signing.key.alias="$KEYSTORE_ALIAS" \
  -Pandroid.injected.signing.key.password="$KEYSTORE_ALIAS_PASSWORD"

# Make a copy of the generated APK and AAB files
outputDir=./custom_build/build_output
mkdir -p "$outputDir"
cp ./tv/build/outputs/apk/debug/tv-debug.apk "$outputDir"/
if [ $installAfterBuild -eq 0 ]; then
  cp ./tv/build/outputs/apk/release/tv-release.apk "$outputDir"/
  cp ./tv/build/outputs/bundle/release/tv-release.aab "$outputDir"/
fi
echo "Build completed successfully. Output at: $(cd "$outputDir" && pwd)"

if [ $installAfterBuild -eq 1 ]; then
  if ! command -v adb >/dev/null 2>&1; then
    echo "Built, but 'adb' isn't on your PATH so it can't be installed."
    echo "Add \$ANDROID_HOME/platform-tools to your PATH, or install the APK manually."
    exit 1
  fi
  # The debug build type appends .debug to the application id, so that's what gets installed.
  debugPackage="$APPLICATION_ID.debug"
  echo "Installing $debugPackage..."
  adb install -r "$outputDir/tv-debug.apk"
  # TV apps are launched through LEANBACK_LAUNCHER; the usual LAUNCHER category isn't declared
  # on release-type builds. Resolve the activity rather than using `monkey`, which injects a
  # random input event along with the launch and can tap something on the first screen.
  launchActivity=$(adb shell cmd package resolve-activity --brief \
    -c android.intent.category.LEANBACK_LAUNCHER "$debugPackage" 2>/dev/null | tail -n 1 | tr -d '\r')
  if [ -n "$launchActivity" ] && [[ "$launchActivity" == */* ]]; then
    adb shell am start -n "$launchActivity" >/dev/null 2>&1
    echo "Launched $launchActivity"
  else
    echo "Installed, but couldn't work out the launch activity. Start it from the TV home screen."
  fi
fi
