package app.eluvio.wallet.data.entities.v2

import app.eluvio.wallet.util.realm.RealmEnum

enum class LoginProviders(override val value: String) : RealmEnum {
    AUTH0("auth0"), ORY("ory");

    companion object {
        fun from(value: String?): LoginProviders {
            return value
                ?.let { entries.firstOrNull { it.value == value } }
                // Default to Auth0 if no provider is defined
                ?: ORY
        }
    }
}
