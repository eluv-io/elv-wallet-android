# Configure a custom build

- Fill in all required values in the `custom_build/config/custom.properties` file.
- Add [Icons](#icons).
- Add a [Keystore](#keystore-and-signing).
- Optionally: you can run `./build.sh -v` inside the `custom_build` folder to verify all required config values and files are in place.
- If planning to use Github Actions to create a build, make sure to commit and push your changes, either to the `main` branch or a new branch.

## Icons

Use [IconKitchen](https://icon.kitchen) to create your app icon and place the `android` and
`androidtv` folders you get from IconKitchen in the `/config` folder here.  
(Make sure you also create an `Android TV Banner` using the IconKitchen tool).

## Keystore and Signing

### Generate a Keystore

The easiest way to create a keystore is through
`Android Studio`, [using these steps](https://developer.android.com/studio/publish/app-signing#generate-key).

If you'd rather use the command line, you can use the `keytool` command that comes with the JDK:  
`keytool -genkey -v -keystore <keystore_name>.keystore -alias <alias_name> -keyalg RSA -keysize 2048 -validity 10000`

For more information on how to use `keytool`, refer to
the [official documentation](https://docs.oracle.com/javase/6/docs/technotes/tools/windows/keytool.html).

### Configure the Keystore

Once you have your keystore, place it in the `/config` folder here and add the following to
`custom.properties`:

```
KEYSTORE_PASSWORD=
KEYSTORE_ALIAS=
KEYSTORE_ALIAS_PASSWORD=
```

The tools will use the first `.keystore` or `.jks` file they find in the `/config` folder,
so make sure you only use one keystore to avoid confusion.
