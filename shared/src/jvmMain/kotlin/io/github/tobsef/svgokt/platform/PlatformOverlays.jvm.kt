package io.github.tobsef.svgokt.platform

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalClipboardManager

/**
 * Draws the desktop confirm/alert dialogs and hands [DesktopPlatform] the live clipboard, so its
 * plain `copyToClipboard`/`confirm`/`alert` calls have something AWT-free to resolve against.
 */
@Composable
public actual fun PlatformOverlays() {
    // Give the object the clipboard once it exists in the composition; recapture is cheap and keeps
    // it valid across recompositions.
    val clipboard = LocalClipboardManager.current
    SideEffect { DesktopPlatform.bindClipboard(clipboard) }

    val request by DesktopPlatform.currentDialog.collectAsState()
    when (val dialog = request) {
        is DialogRequest.Alert -> AlertDialog(
            onDismissRequest = { dialog.answer.complete(Unit) },
            confirmButton = {
                TextButton(onClick = { dialog.answer.complete(Unit) }) { Text("OK") }
            },
            title = { Text(dialog.title) },
            text = { Text(dialog.message) },
        )

        is DialogRequest.Confirm -> AlertDialog(
            onDismissRequest = { dialog.answer.complete(false) },
            confirmButton = {
                TextButton(onClick = { dialog.answer.complete(true) }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { dialog.answer.complete(false) }) { Text("No") }
            },
            title = { Text(dialog.title) },
            text = { Text(dialog.message) },
        )

        null -> Unit
    }
}
