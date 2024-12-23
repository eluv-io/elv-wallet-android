@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.hilt)
    kotlin("kapt")
    alias(libs.plugins.ksp)
    alias(libs.plugins.realm)
    alias(libs.plugins.gradle.secrets)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "app.eluvio.wallet"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.eluvio.wallet"
        minSdk = 23
        targetSdk = 34
        versionCode = 28
        versionName = "2.0"
        // Change version name to include CI build number and version code
        project.ext
            .takeIf { it.has("ci_build_number") }
            ?.get("ci_build_number")
            ?.let { build ->
                versionName = "v${versionName}_${versionCode}-b$build"
            }
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
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
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
    kapt(libs.hilt.android.compiler)

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

    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.reflections)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)

    implementation(libs.installReferrer)

    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.rxjava3)
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}

secrets {
    propertiesFileName = "secrets/secrets.properties"
    defaultPropertiesFileName = "secrets.default.properties"
}

tasks.register("printVersion") {
    println(android.defaultConfig.versionName)
}
