@file:JvmName("StringUtils")

package app.eluvio.wallet.util

import android.text.Spanned
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.core.text.HtmlCompat
import app.eluvio.wallet.util.crypto.Base58
import java.security.MessageDigest

/** Convenience to parse HTML and convert to [Spanned] */
fun String.toHtmlSpan(): Spanned =
    HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY)

/** Convenience to parse HTML and convert to a Compose [AnnotatedString]. */
fun String.toHtmlAnnotated(): AnnotatedString =
    AnnotatedString.fromHtml(this)

fun String.toHexByteArray(): ByteArray {
    check(length % 2 == 0) { "Must have an even length" }
    return removePrefix("0x")
        .chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

// Thanks GPT
fun ByteArray.hexToString(): String {
    val hexChars = "0123456789ABCDEF"
    val result = StringBuilder()
    for (byte in this) {
        val intValue = byte.toInt() and 0xFF
        result.append(hexChars[intValue shr 4])
        result.append(hexChars[intValue and 0x0F])
    }
    return result.toString()
}

val String.base58: String
    get() = Base58.encode(this.toHexByteArray())

val String.sha256: String
    get() = MessageDigest
        .getInstance("SHA-256")
        .digest(toByteArray())
        .toHexString()

val String.sha512: String
    get() = MessageDigest
        .getInstance("SHA-512")
        .digest(toByteArray())
        .toHexString()
