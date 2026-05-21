package app.eluvio.wallet.util

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/**
 * Converts a [Spanned] into an [AnnotatedString] trying to keep as much formatting as possible.
 *
 * Currently supports `bold`, `italic`, `underline` and `color`.
 *
 * @param trim Whether to remove leading and trailing whitespaces.
 *
 * Note about trim: technically, while the return type of [Spanned.trim] is [CharSequence], at
 * runtime, it's actually a [Spanned], so we could just cast it before converting to an
 * [AnnotatedString]. But doing it this way, we aren't relying on any behind-the-scenes knowledge,
 * and sticking to public method signatures.
 */
fun Spanned.toAnnotatedString(trim: Boolean = true): AnnotatedString {
    val spanned = this@toAnnotatedString
    val result = buildAnnotatedString {
        append(spanned)
        getSpans(0, spanned.length, Any::class.java).forEach { span ->
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            when (span) {
                is StyleSpan -> when (span.style) {
                    Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                    Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                    Typeface.BOLD_ITALIC -> addStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic
                        ), start, end
                    )
                }

                is UnderlineSpan -> addStyle(
                    SpanStyle(textDecoration = TextDecoration.Underline),
                    start,
                    end
                )

                is ForegroundColorSpan -> addStyle(
                    SpanStyle(color = Color(span.foregroundColor)),
                    start,
                    end
                )
            }
        }
    }
    return if (trim) result.trim() else result
}

/**
 * Removes trailing and leading whitespaces, while maintaining styles.
 */
fun AnnotatedString.trim(): AnnotatedString {
    val startIndex = indexOfFirst { !it.isWhitespace() }
    val endIndex = indexOfLast { !it.isWhitespace() }
    if (startIndex == -1 || endIndex == -1) return AnnotatedString("")
    return subSequence(startIndex, endIndex + 1)
}
