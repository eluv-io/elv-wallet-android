package app.eluvio.wallet.util

import android.os.Build
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale

object Device {
    val NAME = "${Build.MANUFACTURER.capitalize(Locale.current)} ${Build.MODEL}"
}
