package app.eluvio.wallet.util.compose

import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.PreviewParameterProvider

/**
 * The default configs of some devices in [Devices] use a DPI value that doesn't look like real
 * devices. This is closer to reality.
 */
object RealisticDevices {
    const val TV_720p = "spec:shape=Normal,width=1280,height=720,unit=dp,dpi=320"
}

class BooleanParameterProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(true, false)
}
