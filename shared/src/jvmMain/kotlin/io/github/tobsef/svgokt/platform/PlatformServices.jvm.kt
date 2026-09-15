package io.github.tobsef.svgokt.platform

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import dev.nucleusframework.window.tao.TaoDragAndDropPayload
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Data
import org.jetbrains.skia.Surface
import org.jetbrains.skia.svg.SVGDOM
import org.jetbrains.skia.svg.SVGLengthUnit
import org.jetbrains.skia.svg.SVGPreserveAspectRatio
import org.jetbrains.skia.svg.SVGPreserveAspectRatioAlign
import java.io.File
import java.util.zip.GZIPInputStream
import kotlin.math.roundToInt

private const val CONFIG_DIR = ".svgpaw"
private const val CONFIG_FILE = "config.json"

/**
 * A yes/no or acknowledge dialog the desktop UI must draw itself.
 *
 * Nucleus' Tao backend has no AWT, so there is no JOptionPane to lean on. The model still calls
 * [PlatformServices.confirm]/[PlatformServices.alert] as plain suspend functions; each parks on its
 * [answer] until [DesktopDialogHost] — mounted at the app root — resolves it from a Compose
 * `AlertDialog`.
 */
internal sealed interface DialogRequest {
    val title: String
    val message: String

    data class Confirm(override val title: String, override val message: String) : DialogRequest {
        val answer = CompletableDeferred<Boolean>()
    }

    data class Alert(override val title: String, override val message: String) : DialogRequest {
        val answer = CompletableDeferred<Unit>()
    }
}

/** Desktop (JVM) host: real files, native FileKit dialogs, Skia rasterization, Tao drag & drop. */
internal object DesktopPlatform : PlatformServices {

    // ---- file system ------------------------------------------------------- #

    override suspend fun readSvg(path: String): String = withContext(Dispatchers.IO) {
        val bytes = File(path).readBytes()
        // gzip magic -> .svgz
        val raw = if (bytes.size > 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) {
            GZIPInputStream(bytes.inputStream()).use { it.readBytes() }
        } else {
            bytes
        }
        raw.toString(Charsets.UTF_8)
    }

    override suspend fun writeFile(path: String, text: String): Unit = withContext(Dispatchers.IO) {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(text, Charsets.UTF_8)
    }

    override fun fileExists(path: String): Boolean = File(path).exists()

    override fun fileName(path: String): String = File(path).name

    override fun parentDir(path: String): String = File(path).parent ?: "."

    override fun baseName(path: String): String = File(path).nameWithoutExtension

    override fun join(dir: String, name: String): String = File(dir, name).path

    override fun listSvgFiles(path: String): List<String>? {
        val dir = File(path)
        if (!dir.isDirectory) return null
        return dir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in setOf("svg", "svgz") }
            ?.sortedBy { it.name }
            ?.map { it.path }
            .orEmpty()
    }

    override fun homeDir(): String = System.getProperty("user.home") ?: "."

    // ---- dialogs ------------------------------------------------------------ #

    override suspend fun pickSvgFiles(startDir: String?): List<String> {
        val picked = FileKit.openFilePicker(
            type = FileKitType.File(extensions = setOf("svg", "svgz")),
            mode = FileKitMode.Multiple(),
            directory = startDir.asDirectory(),
        )
        return picked?.map { it.absolutePath() }.orEmpty()
    }

    override suspend fun pickSaveTarget(suggestedName: String, startDir: String?): String? {
        val saved = FileKit.openFileSaver(
            suggestedName = File(suggestedName).nameWithoutExtension,
            extension = "svg",
            directory = startDir.asDirectory(),
        ) ?: return null
        val path = saved.absolutePath()
        return if (File(path).extension.isEmpty()) "$path.svg" else path
    }

    override suspend fun pickDirectory(startDir: String?): String? =
        FileKit.openDirectoryPicker(directory = startDir.asDirectory())?.absolutePath()

    /** FileKit wants an existing directory as its start point, or nothing. */
    private fun String?.asDirectory(): PlatformFile? =
        this?.takeIf { File(it).isDirectory }?.let { PlatformFile(File(it)) }

    // ---- confirm / alert: bridged to a Compose dialog at the app root -------- #

    private val dialogMutex = Mutex()

    /** The dialog currently on screen, observed by [DesktopDialogHost]. One at a time. */
    internal val currentDialog = MutableStateFlow<DialogRequest?>(null)

    override suspend fun confirm(title: String, message: String): Boolean =
        dialogMutex.withLock {
            val request = DialogRequest.Confirm(title, message)
            currentDialog.value = request
            try {
                request.answer.await()
            } finally {
                currentDialog.value = null
            }
        }

    override suspend fun alert(title: String, message: String): Unit =
        dialogMutex.withLock {
            val request = DialogRequest.Alert(title, message)
            currentDialog.value = request
            try {
                request.answer.await()
            } finally {
                currentDialog.value = null
            }
        }

    // ---- clipboard ----------------------------------------------------------- #

    /** Captured from `LocalClipboardManager` at the app root; null until the first frame. */
    @Volatile
    private var clipboard: ClipboardManager? = null

    internal fun bindClipboard(manager: ClipboardManager) {
        clipboard = manager
    }

    override fun copyToClipboard(text: String) {
        clipboard?.setText(AnnotatedString(text))
    }

    // ---- rasterize ----------------------------------------------------------- #

    /**
     * Rasterize through Skia's SVG DOM — the same renderer Compose itself draws with, so the
     * preview matches what a browser shows closely enough to judge an optimization by.
     */
    override fun rasterize(svg: String, maxSize: Int): ImageBitmap? = try {
        val dom = SVGDOM(Data.makeFromBytes(svg.toByteArray(Charsets.UTF_8)))
        val root = dom.root ?: throw IllegalStateException("no <svg> root")

        // An SVG sized in percent, or sized only by its viewBox, reports no intrinsic size; the
        // viewBox is what says how big it really is.
        val viewBox = root.viewBox
        val intrinsicWidth = root.width.takeIf { it.unit != SVGLengthUnit.PERCENTAGE }?.value ?: 0f
        val intrinsicHeight = root.height.takeIf { it.unit != SVGLengthUnit.PERCENTAGE }?.value ?: 0f
        val width = if (intrinsicWidth > 0) intrinsicWidth else viewBox?.width ?: 0f
        val height = if (intrinsicHeight > 0) intrinsicHeight else viewBox?.height ?: 0f
        if (width <= 0 || height <= 0) return null

        val scale = maxSize / maxOf(width, height)
        val pixelWidth = (width * scale).roundToInt().coerceIn(1, 8192)
        val pixelHeight = (height * scale).roundToInt().coerceIn(1, 8192)

        // The container size is what a percent-sized document lays itself out against; the canvas
        // transform is what scales a document with its own units up to the preview resolution.
        // Doing it this way leaves the DOM untouched, so nothing depends on Skia's setters.
        root.preserveAspectRatio = SVGPreserveAspectRatio(SVGPreserveAspectRatioAlign.XMID_YMID)

        val surface = Surface.makeRasterN32Premul(pixelWidth, pixelHeight)
        dom.setContainerSize(width, height)
        surface.canvas.scale(scale, scale)
        dom.render(surface.canvas)
        surface.makeImageSnapshot().toComposeImageBitmap()
    } catch (_: Throwable) {
        // A document Skia cannot render is not an error the user needs to act on — the Markup tab
        // still shows it, and the placeholder says so.
        null
    }

    // ---- config -------------------------------------------------------------- #

    private val configFile: File get() = File(File(homeDir(), CONFIG_DIR), CONFIG_FILE)

    override fun loadConfig(): String? = configFile.takeIf { it.isFile }?.readText(Charsets.UTF_8)

    override fun saveConfig(json: String) {
        configFile.parentFile?.mkdirs()
        configFile.writeText(json, Charsets.UTF_8)
    }

    // ---- drag & drop ----------------------------------------------------------- #

    @OptIn(ExperimentalComposeUiApi::class)
    override fun fileDropTarget(onFiles: (List<String>) -> Unit): Modifier = Modifier.dragAndDropTarget(
        shouldStartDragAndDrop = { true },
        target = object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                // Tao delivers the native drop as a payload of absolute paths, so there is no AWT
                // DataFlavor round-trip and nothing that would boot the AWT toolkit.
                val paths = (event.nativeEvent as? TaoDragAndDropPayload)?.files.orEmpty()
                    .filter { it.substringAfterLast('.').lowercase() in setOf("svg", "svgz") }
                if (paths.isEmpty()) return false
                onFiles(paths)
                return true
            }
        },
    )
}

public actual fun platformServices(): PlatformServices = DesktopPlatform
