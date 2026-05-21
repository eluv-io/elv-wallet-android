package app.eluvio.wallet.util.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A modifier that will apply the given [block] if the given [condition] is true.
 */
@Composable
fun Modifier.thenIf(condition: Boolean, block: @Composable Modifier.() -> Modifier): Modifier {
    return if (condition) then(Modifier.block()) else this
}

fun <T> Modifier.thenIfNotNull(value: T?, block: Modifier.(T) -> Modifier): Modifier {
    return if (value != null) then(Modifier.block(value)) else this
}
