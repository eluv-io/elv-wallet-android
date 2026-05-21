package app.eluvio.wallet.screens.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.Text
import kotlin.math.ceil
import kotlin.math.max

/**
 * A [Text] composable that shrinks to fit its content.
 */
@Composable
fun WrapContentText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
) {
    val measurer = rememberTextMeasurer()
    Layout(
        modifier = modifier,
        content = {
            Text(
                text = text,
                style = style,
                textAlign = textAlign,
                color = color,
                maxLines = maxLines,
                overflow = overflow,
            )
        }) { measurables, constraints ->
        // Measure the actual width the text will take up
        val textWidthPx = measurer.measure(
            text,
            style = style,
            maxLines = maxLines,
            overflow = overflow,
            constraints = constraints
        ).maxLineWidth.toInt()
        // There's only 1 measurable (the Text()), limit it to the width we already know it'll need
        val textPlaceable = measurables.first().measure(constraints.copy(maxWidth = textWidthPx))
        layout(textPlaceable.width, textPlaceable.height) {
            textPlaceable.place(0, 0)
        }
    }
}

private val TextLayoutResult.maxLineWidth: Float
    get() {
        var maxWidth = 0f
        repeat(multiParagraph.lineCount) { line ->
            val lineWidth = multiParagraph.getLineWidth(line)
            maxWidth = max(maxWidth, lineWidth)
        }
        return ceil(maxWidth)
    }
