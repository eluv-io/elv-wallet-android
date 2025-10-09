import org.jetbrains.kotlin.util.prefixIfNot

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.realm)
    alias(libs.plugins.gradle.secrets)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "app.eluvio.wallet"
    compileSdk = 36
    val customBuildConfig = CustomBuildConfig.from(project.ext.properties)
    defaultConfig {
        applicationId = customBuildConfig.applicationId
        minSdk = 23
        targetSdk = 36
        versionCode = customBuildConfig.versionCode
        versionName = customBuildConfig.versionName
        // Change version name to include CI build number and version code
        project.ext
            .takeIf { it.has("ci_build_number") }
            ?.get("ci_build_number")
            ?.let { build ->
                versionName = "${versionName?.prefixIfNot("v")}_${versionCode}-b$build"
            }

        resValue("string", "app_name", customBuildConfig.appName)
        buildConfigField("String", "DEFAULT_PROPERTY_ID", customBuildConfig.defaultPropertyId)
        buildConfigField("boolean", "DEFAULT_TO_STAGING_ENV", "${customBuildConfig.defaultToStaging}")
        buildConfigField("boolean", "DISABLE_PURCHASE_PROMPTS", "${customBuildConfig.disablePurchasePrompts}")
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk.debugSymbolLevel = "FULL"
        }

        getByName("debug") {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    flavorDimensions += listOf("server")
    productFlavors {
        create("default") {
            dimension = "server"
        }
        create("mock") {
            dimension = "server"
            applicationIdSuffix = ".mock"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.androidx.leanback)

    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.rxjava3)

    implementation(libs.accompanist.placeholder.material)

    implementation(libs.compose.destinations)
    ksp(libs.compose.destinations.ksp)

    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.retrofit.rxjava)
    debugImplementation(libs.okhttp.logginginterceptor)
    debugImplementation(libs.ok2curl)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.rxandroid)
    // Because RxAndroid releases are few and far between, it is recommended you also
    // explicitly depend on RxJava's latest version for bug fixes and new features.
    // (see https://github.com/ReactiveX/RxJava/releases for latest 3.x.x version)
    implementation(libs.rxjava)
    implementation(libs.rxkotlin)

    implementation(libs.hilt.navigation.compose)

    implementation(libs.qrcode.kotlin.android)

    implementation(libs.androidx.lifecycle.process)

    implementation(libs.realm)
    implementation(libs.kotlinx.coroutines.rx3)

    ksp(libs.moshi.codegen)
    implementation(libs.moshi.kotlin)
    implementation(libs.moshi.adapters)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.dash)
    implementation(libs.androidx.media3.hls)
    implementation(libs.androidx.media3.okhttp)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.ui.leanback)
    implementation(libs.muxstats)

    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.reflections)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.mockk)
    testImplementation(kotlin("test"))

    implementation(libs.installReferrer)

    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.rxjava3)
}

secrets {
    propertiesFileName = "secrets/secrets.properties"
    defaultPropertiesFileName = "secrets.default.properties"
}

tasks.register("printVersion") {
    println(android.defaultConfig.versionName)
}

// Customize by using ./custom_build/build.sh, otherwise defaults to Eluvio Media Wallet values.
data class CustomBuildConfig(
    // Package name for the final APK/AAB
    val applicationId: String,

    // Application name appears under the app icon
    val appName: String,
    // Optional: If provided, skips the Discover page and launches the app directly to the specified Property.
    val defaultPropertyId: String,

    // Version data for Play Store
    val versionCode: Int,
    val versionName: String,

    // If true, the staging toggle will be on by default.
    val defaultToStaging: Boolean,

    // Don't show QR codes or links to direct users to purchase content.
    // This is for stores that don't allow third party in-app purchases (Amazon).
    val disablePurchasePrompts: Boolean,
) {
    companion object {
        fun from(properties: Map<String, Any?>): CustomBuildConfig {
            val applicationId = properties["applicationId"]?.toString()?.ifEmpty { null }
            val versionCode = properties["versionCode"]?.toString()?.ifEmpty { null }?.toIntOrNull()
            val versionName = properties["versionName"]?.toString()?.ifEmpty { null }
            val appName = properties["appName"]?.toString()?.ifEmpty { null }
            val propertyId = properties["defaultPropertyId"]?.toString()
                ?.ifEmpty { null }
                ?.let { "\"$it\"" }
            val defaultToStaging = properties["defaultToStaging"]?.toString()?.toBooleanStrictOrNull()
            val disablePurchasePrompts = properties["disablePurchasePrompts"]?.toString()?.toBooleanStrictOrNull()

            return CustomBuildConfig(
                applicationId = applicationId ?: "app.eluvio.wallet",
                appName = appName ?: "Media Wallet",
                defaultPropertyId = propertyId ?: "null",
                versionCode = versionCode ?: 33,
                versionName = versionName ?: "2.0.1",
                defaultToStaging = defaultToStaging ?: false,
                disablePurchasePrompts = disablePurchasePrompts ?: false,
            )
        }
    }
}
