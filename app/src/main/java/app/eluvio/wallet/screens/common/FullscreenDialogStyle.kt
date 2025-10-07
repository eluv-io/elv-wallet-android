package app.eluvio.wallet.screens.common

import androidx.compose.ui.window.DialogProperties
import com.ramcosta.composedestinations.spec.DestinationStyle

object FullscreenDialogStyle : DestinationStyle.Dialog() {
    override val properties = DialogProperties(
        usePlatformDefaultWidth = false
    )
}
