package app.eluvio.wallet.screens.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.eluvio.wallet.theme.borders
import app.eluvio.wallet.theme.carousel_48
import app.eluvio.wallet.theme.focusedBorder
import app.eluvio.wallet.util.compose.KeyboardClosedHandler
import app.eluvio.wallet.util.compose.icons.Eluvio
import app.eluvio.wallet.util.compose.icons.Search


@Composable
fun SearchBox(
    query: String,
    hint: String = "",
    onQueryChanged: (String) -> Unit,
    onSearchClicked: () -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    val textFocusRequester = remember { FocusRequester() }
    // Copied from TV Material Text, since BasicTextField is a non-TV component
    val textColor = LocalTextStyle.current.color.takeOrElse {
        LocalContentColor.current
    }

    KeyboardClosedHandler {
        // Prevents the user from having to press Back just to go from textview to Surface
        focusRequester.requestFocus()
    }
    Surface(
        onClick = { textFocusRequester.requestFocus() },
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.Transparent
        ),
        border = MaterialTheme.borders.focusedBorder,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
    ) {
        val hintColor = Color(0xFFDBDBDB)
        BasicTextField(
            value = query,
            onValueChange = onQueryChanged,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(onSearch = { onSearchClicked() }),
            textStyle = MaterialTheme.typography.carousel_48.copy(color = textColor),
            cursorBrush = SolidColor(LocalContentColor.current),
            decorationBox = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Eluvio.Search,
                        tint = hintColor,
                        contentDescription = "Search",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(15.dp))
                    Box(contentAlignment = Alignment.CenterStart) {
                        it()
                        if (query.isEmpty()) {
                            Text(
                                hint,
                                color = hintColor,
                                style = MaterialTheme.typography.carousel_48,
                                modifier = Modifier.graphicsLayer { alpha = 0.6f })
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp)
                .focusRequester(textFocusRequester)
        )
    }
}
