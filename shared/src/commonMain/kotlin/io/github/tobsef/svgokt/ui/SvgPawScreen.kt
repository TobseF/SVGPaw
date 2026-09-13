package io.github.tobsef.svgokt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import io.github.tobsef.svgokt.model.SvgPawModel
import io.github.tobsef.svgokt.ui.theme.Palette

private const val SIDEBAR_WIDTH = 360
private const val FEATURES_WIDTH = 400

/**
 * The whole window: files and globals on the left, the preview in the middle, the plugin list on
 * the right. Splitting the plugin list off is what keeps either sidebar short enough that the
 * preview never has to compete with it for height.
 */
@Composable
public fun SvgPawScreen(model: SvgPawModel, modifier: Modifier = Modifier) {
    // The shortcuts hang off a focusable root: key events that no focused child consumed bubble
    // up to here, which is what keeps Space from stealing the space bar out of a text field.
    val rootFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { rootFocus.requestFocus() }

    Box(
        modifier
            .fillMaxSize()
            .background(Palette.Bg)
            .then(model.platform.fileDropTarget { model.openPaths(it) })
            .focusRequester(rootFocus)
            .focusable()
            .appShortcuts(model),
    ) {
        Row(Modifier.fillMaxSize()) {
            Sidebar(model, Modifier.width(SIDEBAR_WIDTH.dp).fillMaxHeight())
            PreviewPane(model, Modifier.weight(1f).fillMaxHeight())
            FeaturesPanel(model, Modifier.width(FEATURES_WIDTH.dp).fillMaxHeight())
        }

        if (model.settingsDialogOpen) {
            ModalScrim(onDismiss = model::closeSettingsDialog) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Palette.Bg)
                        // Swallow clicks so they don't reach the scrim behind the dialog.
                        .clickable(remember { MutableInteractionSource() }, indication = null) {},
                ) {
                    OutputSettingsDialog(model)
                }
            }
        }
    }
}

/** A dimming overlay that closes on a click outside the dialog. */
@Composable
private fun ModalScrim(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Popup(alignment = Alignment.Center, onDismissRequest = onDismiss) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(remember { MutableInteractionSource() }, indication = null) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

/**
 * Ctrl+O / Ctrl+S / Ctrl+Shift+S and Space, the way the Python front-end bound them.
 *
 * These run on the *preview* pass, before the focused component sees the key. That matters for
 * Space: Compose's `clickable` treats it as an activation, so once any switch or button has been
 * clicked it would eat the space bar instead of letting it flip the preview. The one thing that
 * legitimately owns Space is a text field, and [SvgPawModel.textInputFocused] says when one does.
 */
private fun Modifier.appShortcuts(model: SvgPawModel): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    when {
        event.isCtrlPressed && event.key == Key.O -> {
            model.chooseFiles(); true
        }

        event.isCtrlPressed && event.isShiftPressed && event.key == Key.S -> {
            model.saveAll(); true
        }

        event.isCtrlPressed && event.key == Key.S -> {
            model.saveCurrent(); true
        }

        event.key == Key.Spacebar && !model.textInputFocused && !model.settingsDialogOpen -> {
            model.toggleShowing(); true
        }

        else -> false
    }
}
