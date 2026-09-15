package io.github.tobsef.svgokt.platform

import androidx.compose.runtime.Composable

/**
 * Host for anything a platform has to draw inside the Compose tree rather than hand off to the OS.
 *
 * On desktop that is the confirm/alert dialogs (Nucleus' Tao backend has no AWT JOptionPane) and
 * the clipboard binding; every other target has nothing to draw and gets an empty body. Mount it
 * once at the app root, above the app's own content.
 */
@Composable
public expect fun PlatformOverlays()
