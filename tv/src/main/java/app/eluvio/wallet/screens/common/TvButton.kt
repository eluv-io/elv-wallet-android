package app.eluvio.wallet.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.ClickableSurfaceShape
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.LocalSurfaceScale
import app.eluvio.wallet.theme.label_40
import app.eluvio.wallet.util.compose.requestOnce

@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.label_40,
    colors: ClickableSurfaceColors = ClickableSurfaceDefaults.colors(),
    shape: ClickableSurfaceShape = ClickableSurfaceDefaults.shape(RoundedCornerShape(5.dp)),
    scale: ClickableSurfaceScale = LocalSurfaceScale.current,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 5.dp)
) {
    TvButton(onClick, modifier, colors, shape, scale, enabled) {
        Text(
            text,
            style = textStyle,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(contentPadding)
        )
    }
}

@Composable
fun TvButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ClickableSurfaceColors = ClickableSurfaceDefaults.colors(),
    shape: ClickableSurfaceShape = ClickableSurfaceDefaults.shape(RoundedCornerShape(5.dp)),
    scale: ClickableSurfaceScale = LocalSurfaceScale.current,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        colors = colors,
        shape = shape,
        scale = scale,
        enabled = enabled,
        modifier = modifier,
        content = content
    )
}

@Preview
@Composable
fun TvButtonPreview() = EluvioThemePreview {
    Column(
        Modifier
            .fillMaxSize()
            .wrapContentSize()
            .background(Color.Black)
            .padding(15.dp)
    ) {
        TvButton("Unfocused card", onClick = {})

        Spacer(Modifier.height(20.dp))

        val focusRequester = remember { FocusRequester() }
        TvButton(
            "Focused card", onClick = {},
            Modifier.focusRequester(focusRequester)
        )
        focusRequester.requestOnce()
    }
}
