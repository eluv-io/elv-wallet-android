# Custom Wallet Build
To create a custom build of the Wallet app, make sure to add the required files and values in the 
`/config` folder as described below, and then run:
```shell
./build.sh
```

## TL;DR
* Fill in all required values in the `custom.properties` file located in the `/config` folder.
* Add [Icons](#icons).
* Add a [Keystore](#keystore-and-signing).
* Run `./build.sh` to create APK/AAB files.

## Icons
Use [IconKitchen](https://icon.kitchen) to create your app icon and place the `android` and
`androidtv` folders you get from IconKitchen in the `/config` folder here.  
(Make sure you also create an `Android TV Banner` using the IconKitchen tool).

## Keystore and Signing
### Generate a Keystore
The easiest way to create a keystore is through `Android Studio`, [using these steps](https://developer.android.com/studio/publish/app-signing#generate-key).

If you'd rather use the command line, you can use the `keytool` command that comes with the JDK:  
`keytool -genkey -v -keystore <keystore_name>.keystore -alias <alias_name> -keyalg RSA -keysize 2048 -validity 10000`

For more information on how to use `keytool`, refer to the [official documentation](https://docs.oracle.com/javase/6/docs/technotes/tools/windows/keytool.html).

### Configure the Keystore
Once you have your keystore, place it in the `/config` folder here and add the following to `custom.properties`:
```
KEYSTORE_PASSWORD=
KEYSTORE_ALIAS=
KEYSTORE_ALIAS_PASSWORD=
```
The tools will use the first `.keystore` or `.jks` file they find in the `/config` folder, 
so make sure you only use one keystore to avoid confusion.
