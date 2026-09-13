package io.github.tobsef.svgokt

import com.sun.jna.Native
import com.sun.jna.WString
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions
import java.awt.Window

/**
 * Paints the window's title bar dark on Windows.
 *
 * Neither Java nor Compose exposes this: the frame is drawn by the shell, and the only way to ask
 * for the dark variant is the DWM attribute below. Keeping the native decoration — rather than
 * going undecorated and drawing our own — is what preserves snap layouts, Aero shake and the
 * system's own window animations.
 */
private interface Dwmapi : StdCallLibrary {
    fun DwmSetWindowAttribute(
        hwnd: WinDef.HWND,
        attribute: Int,
        value: IntByReference,
        size: Int,
    ): Int

    companion object {
        val INSTANCE: Dwmapi? by lazy {
            runCatching {
                Native.load("dwmapi", Dwmapi::class.java, W32APIOptions.DEFAULT_OPTIONS)
            }.getOrNull()
        }
    }
}

/** `DWMWA_USE_IMMERSIVE_DARK_MODE` — 20 since Windows 10 20H1, 19 on the builds just before it. */
private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
private const val DWMWA_USE_IMMERSIVE_DARK_MODE_LEGACY = 19

/**
 * Ask the shell for a dark title bar. A no-op anywhere but Windows, and on a Windows too old to
 * know the attribute — in both cases the window simply keeps the system default.
 */
fun applyDarkTitleBar(window: Window) {
    if (!System.getProperty("os.name").orEmpty().startsWith("Windows")) return
    val dwmapi = Dwmapi.INSTANCE ?: return

    runCatching {
        val hwnd = WinDef.HWND(Native.getWindowPointer(window))
        val enabled = IntByReference(1)
        // The newer attribute id first; older builds reject it and answer to the legacy one.
        val result = dwmapi.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, enabled, 4)
        if (result != 0) {
            dwmapi.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE_LEGACY, enabled, 4)
        }
        // The frame is only repainted on the next size change, so a window that is already on
        // screen keeps its light bar until something nudges it.
        val size = window.size
        window.setSize(size.width, size.height + 1)
        window.setSize(size.width, size.height)
    }
}

/**
 * Give the process its own task-bar identity.
 *
 * Without it Windows files the window under the Java launcher that started it, and shows the
 * launcher's icon in the task bar however carefully the window's own icon was set. Must run before
 * the first window appears.
 */
private interface Shell32 : StdCallLibrary {
    fun SetCurrentProcessExplicitAppUserModelID(appId: WString): Int

    companion object {
        // No W32APIOptions here: this entry point has no A/W variants for the mapper to append to.
        val INSTANCE: Shell32? by lazy {
            runCatching { Native.load("shell32", Shell32::class.java) }.getOrNull()
        }
    }
}

fun applyTaskbarIdentity(appId: String) {
    if (!System.getProperty("os.name").orEmpty().startsWith("Windows")) return
    runCatching { Shell32.INSTANCE?.SetCurrentProcessExplicitAppUserModelID(WString(appId)) }
}
