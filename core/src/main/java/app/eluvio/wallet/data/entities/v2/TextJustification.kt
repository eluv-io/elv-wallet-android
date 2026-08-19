package app.eluvio.wallet.data.entities.v2

import app.eluvio.wallet.util.realm.RealmEnum

/**
 * How a section aligns the text of its item cards.
 * The server sends an empty value for the default, which maps to [LEFT].
 */
enum class TextJustification(override val value: String) : RealmEnum {
    LEFT("left"),
    CENTER("center"),
    RIGHT("right")
    ;

    companion object {
        fun from(value: String?): TextJustification {
            return entries.firstOrNull { it.value == value?.lowercase() } ?: LEFT
        }
    }
}
