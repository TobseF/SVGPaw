package io.github.tobsef.svgokt

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import dev.nucleusframework.application.DecoratedWindow
import dev.nucleusframework.application.NucleusBackend
import dev.nucleusframework.application.nucleusApplication
import dev.nucleusframework.window.DecoratedWindowDefaults
import dev.nucleusframework.window.NucleusDecoratedWindowTheme
import dev.nucleusframework.window.TitleBar
import io.github.vinceglb.filekit.FileKit

fun main(args: Array<String>) {
    // FileKit provides the native open/save dialogs (no AWT/Swing). The id decides where per-user
    // directories live across operating systems; keep it stable.
    FileKit.init(appId = "SVGPaw")

    // The Tao backend draws a real, native-decorated window with no AWT dependency, which is what
    // lets the same source compile to a JVM-free GraalVM native image.
    nucleusApplication(backend = NucleusBackend.Tao) {
        // The window/task-bar icon, rendered from the app's own SVG through skiko (no AWT ImageIO).
        // Without it the task bar shows the launcher's default Java icon.
        val density = LocalDensity.current
        val icon = remember(density) {
            useResource("icon.svg") { loadSvgPainter(it, density) }
        }
        val state = rememberWindowState(size = DpSize(1600.dp, 1000.dp))
        // Nucleus' default bar is taller than a typical Windows caption; trim it to a compact height.
        val titleBarStyle = remember {
            DecoratedWindowDefaults.darkTitleBarStyle().let {
                it.copy(metrics = it.metrics.copy(height = 30.dp))
            }
        }

        DecoratedWindow(
            onCloseRequest = ::exitApplication,
            state = state,
            title = "SVGPaw",
            icon = icon,
        ) {
            // DecoratedWindow on Tao does not draw title text by itself — the TitleBar slot is what
            // fills the native bar. isDark keeps the bar dark, which is what the old DWM workaround did.
            val windowScope = this
            NucleusDecoratedWindowTheme(isDark = true, titleBarStyle = titleBarStyle) {
                windowScope.TitleBar {
                    // `align` pins content to the left; the layout policy keeps the window controls
                    // in their own reserved slot on the right, so `fillMaxSize` here would have
                    // overrun them. `title`/`icon` come straight from the DecoratedWindow params.
                    Row(
                        modifier = Modifier.align(Alignment.Start).padding(start = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(title, color = Color(0xFFE6E6E6))
                    }
                }
                App(initialFiles = args.toList())
            }
        }
    }
}
