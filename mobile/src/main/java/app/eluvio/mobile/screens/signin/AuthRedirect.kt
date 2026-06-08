package app.eluvio.mobile.screens.signin

/**
 * Single source of truth for the auth-redirect deep link. A dedicated scheme (rather than the
 * `elvwallet` scheme the TV app already claims for content deep links) keeps the auth
 * `<intent-filter>` narrow and free of collisions.
 *
 * Must stay in sync with the `<data>` entry in `mobile/src/main/AndroidManifest.xml`.
 */
internal object AuthRedirect {
    const val SCHEME = "elvauth"
    const val HOST = "auth-complete"
    const val URL = "$SCHEME://$HOST"
}
