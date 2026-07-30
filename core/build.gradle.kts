@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.realm)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.gradle.secrets)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "app.eluvio.wallet.core"
    compileSdk = 36
    defaultConfig {
        minSdk = 23

        // Feature flags. Customizable per build via -P properties (mirrors :tv), default false.
        val defaultToStaging = (project.findProperty("defaultToStaging") as? String)
            ?.toBooleanStrictOrNull() ?: false
        val disablePurchasePrompts = (project.findProperty("disablePurchasePrompts") as? String)
            ?.toBooleanStrictOrNull() ?: false
        val defaultPropertyId = (project.findProperty("defaultPropertyId") as? String)
            ?.ifEmpty { null }?.let { "\"$it\"" } ?: "null"
        buildConfigField("boolean", "DEFAULT_TO_STAGING_ENV", "$defaultToStaging")
        buildConfigField("boolean", "DISABLE_PURCHASE_PROMPTS", "$disablePurchasePrompts")
        buildConfigField("String", "DEFAULT_PROPERTY_ID", defaultPropertyId)
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
    // Both :tv and :mobile are Compose-based, so :core can expose Compose types in its
    // public API (AnnotatedString in DynamicPageLayoutState, @Immutable stability hints)
    // and host shared Composable helpers (subscribeToState, rememberToaster).
    // `api` so the same Compose runtime version flows through to consumers.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.rxjava3)
    api(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.process)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.realm)

    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.retrofit.rxjava)
    ksp(libs.moshi.codegen)
    implementation(libs.moshi.kotlin)
    implementation(libs.moshi.adapters)

    debugImplementation(libs.okhttp.logginginterceptor)
    debugImplementation(libs.ok2curl)

    implementation(libs.rxandroid)
    implementation(libs.rxjava)
    implementation(libs.rxkotlin)
    implementation(libs.kotlinx.coroutines.rx3)

    api(libs.androidx.datastore)
    api(libs.androidx.datastore.rxjava3)

    api(libs.kotlinx.collections.immutable)
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)

    // Navigation 3 runtime — :core's *NavArgs classes implement `NavKey`, so the type is on the
    // public API. `lifecycle-viewmodel-navigation3` is used by both :tv and :mobile for the
    // ViewModel-scoped entry decorator.
    api(libs.androidx.navigation3.runtime)
    api(libs.androidx.lifecycle.viewmodel.navigation3)

    // nav3-hilt-vm — `@HiltNavArgViewModel` + `@NavArg` annotations (api so :tv/:mobile use them
    // on their own VMs without redeclaring) and the KSP processor that generates one Hilt
    // subclass + entry helper per VM. Each module with annotated VMs needs its own
    // `ksp(libs.stavfx.nav3hiltvm)`. The generated entry helpers call hiltViewModel<VM, F>(), which
    // lives in hilt-navigation-compose — pulled in as api for the same reason.
    api(libs.stavfx.nav3hiltvm.annotations)
    ksp(libs.stavfx.nav3hiltvm.compiler)
    api(libs.hilt.navigation.compose)

    implementation(libs.timber)

    // api so :tv and :mobile can reference Coil interfaces (SingletonImageLoader.Factory)
    // that WalletApplication implements, without pulling in coil-compose (and thus
    // compose-runtime) for non-Compose consumers like :mobile.
    api(libs.coil.base)
    implementation(libs.coil.svg)
    // Coil 3 ships without network support; this wires OkHttp in (via ServiceLoader).
    implementation(libs.coil.network.okhttp)

    // api so consumers can call FirebaseApp.getApps() and FirebaseAnalytics directly without
    // re-declaring the deps. Each app conditionally applies the google-services plugin if a
    // google-services.json is present at build time.
    api(platform(libs.firebase.bom))
    api(libs.firebase.analytics)
    api(libs.firebase.crashlytics)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.dash)
    implementation(libs.androidx.media3.hls)
    implementation(libs.androidx.media3.okhttp)
    // api so :tv / :mobile player code can call monitorWithMuxData on ExoPlayer instances
    // shared from :core (e.g. via a shared player engine) without re-declaring the dep.
    api(libs.muxstats)
}

secrets {
    propertiesFileName = "secrets/secrets.properties"
    defaultPropertiesFileName = "secrets.default.properties"
}
