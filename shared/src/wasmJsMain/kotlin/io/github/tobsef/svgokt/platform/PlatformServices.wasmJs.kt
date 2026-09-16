@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.github.tobsef.svgokt.platform

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skia.Data
import org.jetbrains.skia.Surface
import org.jetbrains.skia.svg.SVGDOM
import org.jetbrains.skia.svg.SVGLengthUnit
import org.jetbrains.skia.svg.SVGPreserveAspectRatio
import org.jetbrains.skia.svg.SVGPreserveAspectRatioAlign
import org.w3c.dom.DragEvent
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.dom.events.Event
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.File
import org.w3c.files.FileReader
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.JsAny
import kotlin.js.JsString
import kotlin.js.toJsArray
import kotlin.js.toJsString
import kotlin.math.roundToInt

/**
 * Browser host. There is no real filesystem here: files opened via the picker or dropped onto the
 * window are read straight into memory and looked back up by a synthetic `"<id>/<name>"` path, and
 * "saving" triggers a download instead of a write. Settings persist to `localStorage`.
 */
internal object BrowserPlatform : PlatformServices {
    private const val CONFIG_KEY = "svgpaw.config"

    private val store = mutableMapOf<String, String>()
    private var nextId = 0
    private val scope = CoroutineScope(Dispatchers.Default)

    private fun register(name: String, text: String): String {
        val path = "${nextId++}/$name"
        store[path] = text
        return path
    }

    // ---- file system ------------------------------------------------------- #

    override suspend fun readSvg(path: String): String =
        store[path] ?: throw IllegalStateException("${fileName(path)} is no longer available; re-open it.")

    override suspend fun writeFile(path: String, text: String): Unit = download(fileName(path), text)

    override fun fileExists(path: String): Boolean = false

    override fun fileName(path: String): String = path.substringAfterLast('/')

    override fun parentDir(path: String): String = path.substringBeforeLast('/', "")

    override fun baseName(path: String): String = fileName(path).substringBeforeLast('.')

    override fun join(dir: String, name: String): String = if (dir.isEmpty()) name else "$dir/$name"

    override fun listSvgFiles(path: String): List<String>? = null

    override fun homeDir(): String = ""

    // ---- dialogs ------------------------------------------------------------ #

    override suspend fun pickSvgFiles(startDir: String?): List<String> =
        awaitFilePicker().map { register(it.name, readFileAsText(it)) }

    override suspend fun pickSaveTarget(suggestedName: String, startDir: String?): String? = suggestedName

    override suspend fun pickDirectory(startDir: String?): String? = null

    override suspend fun confirm(title: String, message: String): Boolean = window.confirm(message)

    override suspend fun alert(title: String, message: String) {
        window.alert(message)
    }

    // ---- misc ------------------------------------------------------------- #

    override fun copyToClipboard(text: String) {
        window.navigator.clipboard.writeText(text)
    }

    /** Rasterizes through the same Skia build Compose itself uses to draw the canvas. */
    override fun rasterize(svg: String, maxSize: Int): ImageBitmap? = try {
        val dom = SVGDOM(Data.makeFromBytes(svg.encodeToByteArray()))
        val root = dom.root ?: throw IllegalStateException("no <svg> root")

        val viewBox = root.viewBox
        val intrinsicWidth = root.width.takeIf { it.unit != SVGLengthUnit.PERCENTAGE }?.value ?: 0f
        val intrinsicHeight = root.height.takeIf { it.unit != SVGLengthUnit.PERCENTAGE }?.value ?: 0f
        val width = if (intrinsicWidth > 0) intrinsicWidth else viewBox?.width ?: 0f
        val height = if (intrinsicHeight > 0) intrinsicHeight else viewBox?.height ?: 0f
        if (width <= 0 || height <= 0) return null

        val scale = maxSize / maxOf(width, height)
        val pixelWidth = (width * scale).roundToInt().coerceIn(1, 8192)
        val pixelHeight = (height * scale).roundToInt().coerceIn(1, 8192)

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

    override fun loadConfig(): String? = window.localStorage.getItem(CONFIG_KEY)

    override fun saveConfig(json: String) {
        window.localStorage.setItem(CONFIG_KEY, json)
    }

    // ---- drag & drop --------------------------------------------------------- #

    private var dropInstalled = false

    override fun fileDropTarget(onFiles: (List<String>) -> Unit): Modifier {
        if (!dropInstalled) {
            dropInstalled = true
            val body = document.body
            body?.addEventListener("dragover", { event: Event -> event.preventDefault() })
            body?.addEventListener("drop", { event: Event ->
                event.preventDefault()
                val files = (event as? DragEvent)?.dataTransfer?.files?.asList().orEmpty()
                    .filter { it.name.substringAfterLast('.').lowercase() in setOf("svg", "svgz") }
                if (files.isEmpty()) return@addEventListener
                scope.launch {
                    onFiles(files.map { register(it.name, readFileAsText(it)) })
                }
            })
        }
        // The listeners above cover the whole page, which is all this single-window app ever
        // shows; there is no per-composable native drop target on the web.
        return Modifier
    }

    // ---- helpers --------------------------------------------------------- #

    private suspend fun awaitFilePicker(): List<File> = suspendCancellableCoroutine { cont ->
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.multiple = true
        input.accept = ".svg,.svgz"
        input.style.display = "none"

        fun finish(files: List<File>) {
            document.body?.removeChild(input)
            if (cont.isActive) cont.resume(files)
        }

        input.addEventListener("change", { _: Event -> finish(input.files?.asList().orEmpty()) })
        // Fires when the user dismisses the native dialog without picking anything.
        input.addEventListener("cancel", { _: Event -> finish(emptyList()) })
        cont.invokeOnCancellation { document.body?.removeChild(input) }

        document.body!!.appendChild(input)
        input.click()
    }

    private suspend fun readFileAsText(file: File): String = suspendCancellableCoroutine { cont ->
        val reader = FileReader()
        reader.onload = {
            val text = (reader.result as? JsString)?.toString().orEmpty()
            if (cont.isActive) cont.resume(text)
        }
        reader.onerror = {
            if (cont.isActive) cont.resumeWithException(IllegalStateException("Could not read ${file.name}"))
        }
        reader.readAsText(file)
    }

    private fun download(name: String, text: String) {
        val blob = Blob(arrayOf<JsAny?>(text.toJsString()).toJsArray(), BlobPropertyBag(type = "image/svg+xml"))
        val url = URL.createObjectURL(blob)
        val anchor = document.createElement("a") as HTMLAnchorElement
        anchor.href = url
        anchor.download = name
        anchor.style.display = "none"
        document.body!!.appendChild(anchor)
        anchor.click()
        document.body!!.removeChild(anchor)
        URL.revokeObjectURL(url)
    }
}

public actual fun platformServices(): PlatformServices = BrowserPlatform
