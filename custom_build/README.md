# Custom Wallet Build (Android)
## Getting Started
1. [Import the project](docs/Importing.md)
2. [Configure your build](docs/Configuration.md)
3. Build it:
    * [Android Studio](docs/AndroidStudio.md) - Build locally with minimal command line usage.
    * Command line - `./build.sh` from this folder. Requires setting up Java and Android SDK
      manually (not covered in this guide).

While you're iterating on branding, `./build.sh -i` builds just the debug APK and installs and
launches it on the connected device - seconds rather than minutes, since it skips the release
build and the app bundle. `./build.sh -h` lists the options.

Builds run on your machine. Your signing key and its passwords are git-ignored and never leave
it - see [Keystore and Signing](docs/Configuration.md#keystore-and-signing).
