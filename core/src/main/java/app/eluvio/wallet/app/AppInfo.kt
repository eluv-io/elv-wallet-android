package app.eluvio.wallet.app

/**
 * Identity of the host application for use by :core code that needs values like the version
 * string or applicationId without depending on a generated `BuildConfig`. Provided via Hilt by
 * the host (`:app`) module, which is the source of truth for these.
 */
data class AppInfo(
    val applicationId: String,
    val versionName: String,
    val versionCode: Int,
    val isDebug: Boolean,
)
