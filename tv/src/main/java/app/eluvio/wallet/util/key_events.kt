package app.eluvio.wallet.util

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/**
 * Convenience method to check if a [KeyEvent] is a key up event for a specific [Key].
 */
fun KeyEvent.isKeyUpOf(key: Key) = this.type == KeyEventType.KeyUp && this.key == key
