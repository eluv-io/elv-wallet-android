@file:JvmName("StringUtils")
package app.eluvio.wallet.util

import app.eluvio.wallet.util.crypto.Base58

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
