package app.eluvio.wallet.screens.common

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.eluvio.wallet.theme.EluvioTheme

@Preview
@Composable
fun EluvioLoadingSpinner(modifier: Modifier = Modifier) = ChasingDots(modifier)

/**
 * Convenience to get a consistent loading spinner for legacy views
 */
class EluvioLoadingSpinner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(
    context, attrs, defStyleAttr
) {
    @Composable
    override fun Content() {
        EluvioTheme {
            EluvioLoadingSpinner(Modifier.padding(20.dp))
        }
    }
}
