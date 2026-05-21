package app.eluvio.wallet.util

import android.graphics.Bitmap
import android.util.Base64
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * ThumbHash decoder - decodes a base64-encoded thumbhash string into a Bitmap placeholder.
 * Based on the reference implementation at https://github.com/evanw/thumbhash
 */
object ThumbHash {

    /**
     * Decodes a base64-encoded thumbhash string to a Bitmap.
     * Returns null if the input is invalid.
     */
    fun decode(base64Hash: String?): Bitmap? {
        if (base64Hash.isNullOrEmpty()) return null
        return try {
            val hash = Base64.decode(base64Hash, Base64.DEFAULT)
            thumbHashToRGBA(hash)?.let { (rgba, w, h) ->
                Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
                    val pixels = IntArray(w * h)
                    for (i in pixels.indices) {
                        val r = rgba[i * 4]
                        val g = rgba[i * 4 + 1]
                        val b = rgba[i * 4 + 2]
                        val a = rgba[i * 4 + 3]
                        pixels[i] = (a.toInt() and 0xFF shl 24) or
                                (r.toInt() and 0xFF shl 16) or
                                (g.toInt() and 0xFF shl 8) or
                                (b.toInt() and 0xFF)
                    }
                    setPixels(pixels, 0, w, 0, 0, w, h)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private data class RGBAResult(val rgba: ByteArray, val width: Int, val height: Int)

    private fun thumbHashToRGBA(hash: ByteArray): RGBAResult? {
        if (hash.size < 5) return null

        val header = hash[0].toInt() and 0xFF or
                ((hash[1].toInt() and 0xFF) shl 8) or
                ((hash[2].toInt() and 0xFF) shl 16)

        val lDc = header and 63
        val pDc = (header shr 6) and 63
        val qDc = (header shr 12) and 63
        val lScale = (header shr 18) and 31
        val hasAlpha = (header shr 23) and 1 != 0

        val pScale = (hash[3].toInt() and 0xFF) and 63
        val qScale = ((hash[3].toInt() and 0xFF) shr 6) or (((hash[4].toInt() and 0xFF) and 15) shl 2)
        val isLandscape = ((hash[4].toInt() and 0xFF) shr 4) and 1 != 0

        val lx = max(3, if (isLandscape) if (hasAlpha) 5 else 7 else if (hasAlpha) 3 else 5)
        val ly = max(3, if (isLandscape) if (hasAlpha) 3 else 5 else if (hasAlpha) 5 else 7)

        var aDc = 1.0f
        var aScale = 0

        if (hasAlpha) {
            aDc = ((hash[4].toInt() and 0xFF) shr 5) / 7.0f
            aScale = hash[5].toInt() and 0xFF
        }

        val acStart = if (hasAlpha) 6 else 5
        var acIndex = 0

        fun decodeChannel(nx: Int, ny: Int, scale: Float): FloatArray {
            val ac = FloatArray(nx * ny)
            for (cy in 0 until ny) {
                for (cx in 0 until nx) {
                    if (cx == 0 && cy == 0) continue
                    val bitIndex = acIndex
                    acIndex++
                    val byteIndex = acStart + (bitIndex shr 1)
                    if (byteIndex >= hash.size) {
                        ac[cx + cy * nx] = 0f
                        continue
                    }
                    val data = (hash[byteIndex].toInt() and 0xFF) shr ((bitIndex and 1) * 4)
                    ac[cx + cy * nx] = ((data and 15) / 7.5f - 1.0f) * scale
                }
            }
            return ac
        }

        val lAc = decodeChannel(lx, ly, lScale / 31.0f)
        val pAc = decodeChannel(3, 3, pScale / 63.0f)
        val qAc = decodeChannel(3, 3, qScale / 63.0f)
        val aAc = if (hasAlpha) decodeChannel(5, 5, aScale / 255.0f) else null

        val ratio = thumbHashToApproximateAspectRatio(hash)
        val w = round(if (ratio > 1) 32.0f else 32.0f * ratio).toInt()
        val h = round(if (ratio > 1) 32.0f / ratio else 32.0f).toInt()

        val rgba = ByteArray(w * h * 4)
        val cxStop = max(lx, if (hasAlpha) 5 else 3)
        val cyStop = max(ly, if (hasAlpha) 5 else 3)

        val fx = FloatArray(cxStop)
        val fy = FloatArray(cyStop)

        for (y in 0 until h) {
            for (x in 0 until w) {
                var l = (lDc / 63.0f)
                var p = (pDc / 31.5f - 1.0f)
                var q = (qDc / 31.5f - 1.0f)
                var a = aDc

                for (cx in 0 until cxStop) {
                    fx[cx] = cos(PI / w * (x + 0.5) * cx).toFloat()
                }
                for (cy in 0 until cyStop) {
                    fy[cy] = cos(PI / h * (y + 0.5) * cy).toFloat()
                }

                for (cy in 0 until ly) {
                    val fy2 = fy[cy] * 2
                    for (cx in 0 until lx) {
                        l += lAc[cx + cy * lx] * fx[cx] * fy2
                    }
                }

                for (cy in 0 until 3) {
                    val fy2 = fy[cy] * 2
                    for (cx in 0 until 3) {
                        val f = fx[cx] * fy2
                        p += pAc[cx + cy * 3] * f
                        q += qAc[cx + cy * 3] * f
                    }
                }

                if (hasAlpha && aAc != null) {
                    for (cy in 0 until 5) {
                        val fy2 = fy[cy] * 2
                        for (cx in 0 until 5) {
                            a += aAc[cx + cy * 5] * fx[cx] * fy2
                        }
                    }
                }

                val b = l - 2.0f / 3.0f * p
                val r = (3.0f * l - b + q) / 2.0f
                val g = r - q

                val idx = (x + y * w) * 4
                rgba[idx] = (max(0f, min(1f, r)) * 255f).toInt().toByte()
                rgba[idx + 1] = (max(0f, min(1f, g)) * 255f).toInt().toByte()
                rgba[idx + 2] = (max(0f, min(1f, b)) * 255f).toInt().toByte()
                rgba[idx + 3] = (max(0f, min(1f, a)) * 255f).toInt().toByte()
            }
        }

        return RGBAResult(rgba, w, h)
    }

    private fun thumbHashToApproximateAspectRatio(hash: ByteArray): Float {
        if (hash.size < 5) return 1f
        val header = (hash[3].toInt() and 0xFF) or ((hash[4].toInt() and 0xFF) shl 8)
        val hasAlpha = ((hash[2].toInt() and 0xFF) shr 7) != 0
        val isLandscape = (header shr 12) and 1 != 0
        val lx = if (isLandscape) if (hasAlpha) 5 else 7 else if (hasAlpha) 3 else 5
        val ly = if (isLandscape) if (hasAlpha) 3 else 5 else if (hasAlpha) 5 else 7
        return lx.toFloat() / ly.toFloat()
    }
}
