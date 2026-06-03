package app.eluvio.mobile.navigation

import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/**
 * Renders the topmost entry in a translucent Compose [Dialog] and cross-fades its content on
 * both enter and exit, suppressing the platform's default dialog window animation.
 *
 * The built-in `DialogSceneStrategy` doesn't expose any animation hooks, and Nav3's
 * `NavDisplay.TransitionKey` metadata only drives the main `AnimatedContent` that swaps
 * scenes — overlay scenes (dialogs, bottom sheets) are rendered separately, above that
 * AnimatedContent. The framework's hook for overlay transitions is [OverlayScene.onRemove],
 * which is what we use here for the exit fade.
 *
 * Tag an entry's metadata with the result of [dialog] to opt in.
 */
class FadeDialogSceneStrategy<T : Any>(
    private val durationMillis: Int = 300,
) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(
        entries: List<NavEntry<T>>
    ): Scene<T>? {
        val entry = entries.lastOrNull() ?: return null
        val properties = entry.metadata[DialogKey] ?: return null
        return FadeDialogScene(
            key = entry.contentKey,
            entry = entry,
            overlaid = entries.dropLast(1),
            dialogProperties = properties,
            durationMillis = durationMillis,
            onBack = onBack,
        )
    }

    companion object {
        object DialogKey : NavMetadataKey<DialogProperties>

        fun dialog(properties: DialogProperties = DialogProperties()): Map<String, Any> =
            metadata { put(DialogKey, properties) }

        fun fullScreenDialog(properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false)): Map<String, Any> =
            metadata { put(DialogKey, properties) }
    }
}

private class FadeDialogScene<T : Any>(
    override val key: Any,
    private val entry: NavEntry<T>,
    private val overlaid: List<NavEntry<T>>,
    private val dialogProperties: DialogProperties,
    private val durationMillis: Int,
    private val onBack: () -> Unit,
) : OverlayScene<T> {

    override val entries: List<NavEntry<T>> = listOf(entry)
    override val previousEntries: List<NavEntry<T>> = overlaid
    override val overlaidEntries: List<NavEntry<T>> = overlaid

    // Held on the scene (not via `remember`) so the same instance survives recomposition of
    // the dialog content and stays addressable from [onRemove], which runs outside the
    // Composable.
    private val alpha = Animatable(0f)

    override val content: @Composable () -> Unit = {
        Dialog(onDismissRequest = onBack, properties = dialogProperties) {
            ConfigureWindow()
            LaunchedEffect(Unit) {
                alpha.animateTo(1f, tween(durationMillis))
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .alpha(alpha.value)
            ) {
                entry.Content()
            }
        }
    }

    override suspend fun onRemove() {
        alpha.animateTo(0f, tween(durationMillis))
    }
}

@Composable
private fun ConfigureWindow() {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        // Strip platform dim + slide-up so the Compose-side fade is the only animation
        // contributing to the visual transition.
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setWindowAnimations(0)
    }
}
