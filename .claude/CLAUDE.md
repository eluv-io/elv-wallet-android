# Eluvio Wallet Android

## Build Commands

Build and run on emulator:
```
./gradlew installDefaultDebug 2>&1 | tail -10 && adb shell monkey -p app.eluvio.wallet.debug -c android.intent.category.LAUNCHER 1
```

## Project Info

- Package name (debug): `app.eluvio.wallet.debug`
- Language: Kotlin
- Build system: Gradle
- Architecture: MVVM with Hilt dependency injection
- Database: Realm
- Reactive: RxJava3
- Navigation: Compose Destinations
