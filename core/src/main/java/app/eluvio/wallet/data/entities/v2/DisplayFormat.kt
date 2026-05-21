package app.eluvio.wallet.data.entities.v2

import app.eluvio.wallet.util.realm.RealmEnum

enum class DisplayFormat(override val value: String) : RealmEnum {
    UNKNOWN("unknown"),
    CAROUSEL("carousel"),
    GRID("grid"),
    BANNER("banner")
    ;

    companion object {
        fun from(value: String?): DisplayFormat {
            return entries.firstOrNull { it.value == value } ?: UNKNOWN
        }
    }
}
