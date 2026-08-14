package app.eluvio.wallet.data.entities.v2

import app.eluvio.wallet.util.realm.RealmEnum

/**
 * Card size for a section, applied to every card in that section.
 * The server sends an empty value for the default size, which maps to [MEDIUM].
 */
enum class CardSize(override val value: String) : RealmEnum {
    EXTRA_SMALL("extra_small"),
    SMALL("small"),
    MEDIUM("medium"),
    LARGE("large"),
    EXTRA_LARGE("extra_large")
    ;

    companion object {
        fun from(value: String?): CardSize {
            return entries.firstOrNull { it.value == value?.lowercase() } ?: MEDIUM
        }
    }
}
