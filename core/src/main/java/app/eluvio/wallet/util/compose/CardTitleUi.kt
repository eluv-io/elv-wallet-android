package app.eluvio.wallet.util.compose

import androidx.compose.ui.text.style.TextAlign
import app.eluvio.wallet.data.entities.v2.TextJustification
import app.eluvio.wallet.data.entities.v2.display.DisplaySettings

/**
 * How item cards in this section should align the title drawn below them, or null when the
 * section doesn't show titles at all.
 * A section that defines neither setting gets the server's defaults: a visible, left-aligned
 * title.
 */
val DisplaySettings?.cardTitleAlign: TextAlign?
    get() {
        val display = this ?: return TextAlign.Start
        if (display.showItemTitles == false) {
            return null
        }
        return when (display.textJustification) {
            TextJustification.LEFT -> TextAlign.Start
            TextJustification.CENTER -> TextAlign.Center
            TextJustification.RIGHT -> TextAlign.End
        }
    }
