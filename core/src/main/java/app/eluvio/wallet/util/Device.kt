package app.eluvio.wallet.util

import android.os.Build
import java.util.Locale

object Device {
    val NAME = "${Build.MANUFACTURER.replaceFirstChar { it.titlecase(Locale.getDefault()) }} ${Build.MODEL}"
}
