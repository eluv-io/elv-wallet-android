package app.eluvio.wallet.screens.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import app.eluvio.wallet.util.logging.Log
import io.github.g0dkar.qrcode.QRCode
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.ByteArrayOutputStream

// TODO: evey time we generate a QR code, we need to use a real size and not this hardcoded hack
const val DEFAULT_QR_SIZE = 512

fun generateQrCode(url: String, size: Int = DEFAULT_QR_SIZE): Single<Bitmap> {
    return Single.fromCallable {
        generateQrCodeBlocking(url, size).also { Log.d("QR generated for url: $url") }
    }
        .subscribeOn(Schedulers.io())
}

fun generateQrCodeBlocking(url: String, size: Int = DEFAULT_QR_SIZE): Bitmap {
    val bytes = ByteArrayOutputStream()
    val qr = QRCode(url)
    val rawData = qr.encode()
    val margin = 20 //(pixels)
    val cellSize = (size - margin) / rawData.size
    qr.render(margin = margin, cellSize = cellSize, rawData = rawData).writeImage(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes.toByteArray(), 0, bytes.size())
    return bitmap
}
