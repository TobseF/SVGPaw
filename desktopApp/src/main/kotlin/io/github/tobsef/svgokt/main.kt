package io.github.tobsef.svgokt

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.tobsef.svgokt.ui.theme.Palette
import javax.swing.UIManager

/** Task-bar identity. Keep it stable — Windows pins and groups windows by this string. */
private const val APP_ID = "Tobse.SVGPaw.1"

fun main(args: Array<String>) {
    // The file choosers are Swing; without the native look-and-feel they show up in Metal, which
    // looks nothing like the rest of the window.
    runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }
    // Has to happen before the first window exists, or the task bar has already filed us away.
    applyTaskbarIdentity(APP_ID)

    application {
        val state = rememberWindowState(
            size = DpSize(1600.dp, 1000.dp),
            position = WindowPosition(Alignment.Center),
        )
        Window(
            onCloseRequest = ::exitApplication,
            state = state,
            title = "SVGPaw",
        ) {
            val icons = remember { loadAppIcons() }

            window.minimumSize = java.awt.Dimension(1280, 800)
            // Without this the frame flashes its default light grey before the first Compose frame.
            window.background = java.awt.Color(
                (Palette.Bg.red * 255).toInt(),
                (Palette.Bg.green * 255).toInt(),
                (Palette.Bg.blue * 255).toInt(),
            )
            if (icons.isNotEmpty()) window.iconImages = icons

            LaunchedEffect(Unit) { applyDarkTitleBar(window) }

            App(initialFiles = args.toList())
        }
    }
}
