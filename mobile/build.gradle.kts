@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.realm)
    alias(libs.plugins.compose.compiler)
}

// Firebase plugins fail the build if mobile/google-services.json is missing.
// The file is git-ignored and fetched on demand via bin/fetch-secrets.sh.
// External contributors and CI builds without the secret skip Firebase entirely.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
} else {
    logger.warn("google-services.json missing — Firebase disabled for this build. Run bin/fetch-secrets.sh to enable.")
}

android {
    namespace = "app.eluvio.mobile"
    compileSdk = 36
    defaultConfig {
        applicationId = "app.eluvio.mobile"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isDebuggable = true
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
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.core.ktx)
    // Kept for the XML activity theme parent (Theme.Material3.Dark.NoActionBar). The app
    // itself is Compose end-to-end; this lib just supplies the activity-level theme.
    implementation(libs.material)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.realm)

    implementation(libs.rxjava)
    implementation(libs.rxandroid)
    implementation(libs.rxkotlin)

    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.media3.ui)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.rxjava3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.coil.compose)
}

tasks.register("printVersion") {
    println(android.defaultConfig.versionName)
}
