package io.github.tobsef.svgokt.platform

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Data
import org.jetbrains.skia.Surface
import org.jetbrains.skia.svg.SVGDOM
import org.jetbrains.skia.svg.SVGLengthUnit
import org.jetbrains.skia.svg.SVGPreserveAspectRatio
import org.jetbrains.skia.svg.SVGPreserveAspectRatioAlign
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File
import java.util.zip.GZIPInputStream
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.roundToInt

private const val CONFIG_DIR = ".svgpaw"
private const val CONFIG_FILE = "config.json"

/** Desktop (JVM) host: real files, Swing dialogs, Skia rasterization, AWT drag & drop. */
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

    override suspend fun pickSvgFiles(startDir: String?): List<String> = onSwing {
        val chooser = JFileChooser(startDir?.takeIf { File(it).isDirectory } ?: homeDir()).apply {
            dialogTitle = "Open SVG files"
            isMultiSelectionEnabled = true
            fileFilter = FileNameExtensionFilter("SVG images (*.svg, *.svgz)", "svg", "svgz")
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFiles.map { it.path }
        } else {
            emptyList()
        }
    }

    override suspend fun pickSaveTarget(suggestedName: String, startDir: String?): String? = onSwing {
        val chooser = JFileChooser(startDir?.takeIf { File(it).isDirectory } ?: homeDir()).apply {
            dialogTitle = "Save optimized SVG as"
            selectedFile = File(startDir ?: homeDir(), suggestedName)
            fileFilter = FileNameExtensionFilter("SVG image (*.svg)", "svg")
        }
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return@onSwing null
        val chosen = chooser.selectedFile
        if (chosen.extension.isEmpty()) File(chosen.path + ".svg").path else chosen.path
    }

    override suspend fun pickDirectory(startDir: String?): String? = onSwing {
        val chooser = JFileChooser(startDir?.takeIf { File(it).isDirectory } ?: homeDir()).apply {
            dialogTitle = "Choose output folder"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.path
        } else {
            null
        }
    }

    override suspend fun confirm(title: String, message: String): Boolean = onSwing {
        JOptionPane.showConfirmDialog(
            null, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
        ) == JOptionPane.YES_OPTION
    }

    override suspend fun alert(title: String, message: String): Unit = onSwing {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE)
    }

    // ---- misc ---------------------------------------------------------------- #

    override fun copyToClipboard(text: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }

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
                val paths = event.droppedFilePaths() ?: return false
                if (paths.isEmpty()) return false
                onFiles(paths)
                return true
            }
        },
    )

    @OptIn(ExperimentalComposeUiApi::class)
    private fun DragAndDropEvent.droppedFilePaths(): List<String>? = try {
        val transferable = awtTransferable
        if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            @Suppress("UNCHECKED_CAST")
            (transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>).map { it.path }
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }

    /** Swing dialogs must run on the EDT; the caller just awaits the answer. */
    private suspend fun <T> onSwing(block: () -> T): T = withContext(Dispatchers.Main) { block() }
}

public actual fun platformServices(): PlatformServices = DesktopPlatform
