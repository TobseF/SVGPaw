package io.github.tobsef.svgokt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.github.tobsef.svgokt.model.SvgPawModel
import io.github.tobsef.svgokt.platform.PlatformOverlays
import io.github.tobsef.svgokt.platform.platformServices
import io.github.tobsef.svgokt.ui.SvgPawScreen
import io.github.tobsef.svgokt.ui.theme.SvgPawTheme

@Composable
public fun App(initialFiles: List<String> = emptyList()) {
    val scope = rememberCoroutineScope()
    val model = remember {
        SvgPawModel(platformServices(), scope).also {
            if (initialFiles.isNotEmpty()) it.openPaths(initialFiles)
        }
    }
    SvgPawTheme {
        SvgPawScreen(model)
        // Confirm/alert dialogs and the clipboard binding the desktop host needs; a no-op elsewhere.
        PlatformOverlays()
    }
}
