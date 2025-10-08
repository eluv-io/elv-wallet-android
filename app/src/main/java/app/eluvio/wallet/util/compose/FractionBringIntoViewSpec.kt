package app.eluvio.wallet.util.compose

import androidx.compose.foundation.gestures.BringIntoViewSpec
import kotlin.math.abs

class FractionBringIntoViewSpec(
    private val parentFraction: Float = 0.3f,
    private val childFraction: Float = 0.0f,
    private val parentStartOffsetPx: Int = 0,
) : BringIntoViewSpec {

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val leadingEdgeOfItemRequestingFocus = offset
        val trailingEdgeOfItemRequestingFocus = offset + size

        val sizeOfItemRequestingFocus =
            abs(trailingEdgeOfItemRequestingFocus - leadingEdgeOfItemRequestingFocus)
        val childSmallerThanParent = sizeOfItemRequestingFocus <= containerSize
        val initialTargetForLeadingEdge =
            (parentFraction * containerSize) +
                    parentStartOffsetPx -
                    (childFraction * sizeOfItemRequestingFocus)
        val spaceAvailableToShowItem = containerSize - initialTargetForLeadingEdge

        val targetForLeadingEdge =
            if (childSmallerThanParent && spaceAvailableToShowItem < sizeOfItemRequestingFocus) {
                containerSize - sizeOfItemRequestingFocus
            } else {
                initialTargetForLeadingEdge
            }

        return leadingEdgeOfItemRequestingFocus - targetForLeadingEdge
    }
}
