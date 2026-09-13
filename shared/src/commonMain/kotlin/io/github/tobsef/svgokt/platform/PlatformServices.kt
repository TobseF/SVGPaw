package io.github.tobsef.svgokt.platform

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Everything the app needs from the host it runs on: file access, dialogs, the clipboard, a
 * settings store and an SVG rasterizer.
 *
 * The desktop implementation is the real one; the other targets get stubs, because the first
 * iteration ships Windows only.
 */
public interface PlatformServices {

    // ---- file system ----------------------------------------------------- #

    /** Read an `.svg` file, transparently decompressing `.svgz`. */
    public suspend fun readSvg(path: String): String

    public suspend fun writeFile(path: String, text: String)

    public fun fileExists(path: String): Boolean

    /** `C:\a\b\icon.svg` → `icon.svg`. */
    public fun fileName(path: String): String

    /** `C:\a\b\icon.svg` → `C:\a\b`. */
    public fun parentDir(path: String): String

    /** `C:\a\b\icon.svg` → `icon`. */
    public fun baseName(path: String): String

    public fun join(dir: String, name: String): String

    /** The `.svg`/`.svgz` files directly inside [path], or `null` when it is not a directory. */
    public fun listSvgFiles(path: String): List<String>?

    public fun homeDir(): String

    // ---- dialogs ---------------------------------------------------------- #

    /** Native open dialog, multi-select. Empty when the user cancelled. */
    public suspend fun pickSvgFiles(startDir: String?): List<String>

    /** Native save dialog. `null` when the user cancelled. */
    public suspend fun pickSaveTarget(suggestedName: String, startDir: String?): String?

    public suspend fun pickDirectory(startDir: String?): String?

    /** Yes/no question. Used before replacing a file that already exists. */
    public suspend fun confirm(title: String, message: String): Boolean

    public suspend fun alert(title: String, message: String)

    // ---- misc ------------------------------------------------------------- #

    public fun copyToClipboard(text: String)

    /**
     * Rasterize [svg] so its long edge is [maxSize] pixels. `null` when the document cannot be
     * rendered — the file itself may still be perfectly fine, which is why the UI offers Markup.
     */
    public fun rasterize(svg: String, maxSize: Int): ImageBitmap?

    /** Persisted app config, as a JSON string. `null` on first start. */
    public fun loadConfig(): String?

    public fun saveConfig(json: String)

    /** Accept SVG files dropped onto a composable. A no-op Modifier where drops aren't supported. */
    public fun fileDropTarget(onFiles: (List<String>) -> Unit): Modifier
}

/** The host this build runs on. */
public expect fun platformServices(): PlatformServices

/** Long edge the preview bitmap is rasterized at. Zooming scales this bitmap. */
public const val RENDER_SIZE: Int = 1024
