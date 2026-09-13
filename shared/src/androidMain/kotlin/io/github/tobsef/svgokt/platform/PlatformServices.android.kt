package io.github.tobsef.svgokt.platform

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Android stub. The first iteration ships the desktop app only; this keeps the target compiling
 * until it gets a host implementation of its own.
 */
internal object StubPlatform : PlatformServices {
    private fun nope(): Nothing = throw UnsupportedOperationException("Not available on Android.")

    override suspend fun readSvg(path: String): String = nope()
    override suspend fun writeFile(path: String, text: String): Unit = nope()
    override fun fileExists(path: String): Boolean = false
    override fun fileName(path: String): String = path.substringAfterLast('/').substringAfterLast('\')
    override fun parentDir(path: String): String = path.substringBeforeLast('/', "")
    override fun baseName(path: String): String = fileName(path).substringBeforeLast('.')
    override fun join(dir: String, name: String): String = if (dir.isEmpty()) name else "$dir/$name"
    override fun listSvgFiles(path: String): List<String>? = null
    override fun homeDir(): String = ""

    override suspend fun pickSvgFiles(startDir: String?): List<String> = emptyList()
    override suspend fun pickSaveTarget(suggestedName: String, startDir: String?): String? = null
    override suspend fun pickDirectory(startDir: String?): String? = null
    override suspend fun confirm(title: String, message: String): Boolean = false
    override suspend fun alert(title: String, message: String) {}

    override fun copyToClipboard(text: String) {}
    override fun rasterize(svg: String, maxSize: Int): ImageBitmap? = null
    override fun loadConfig(): String? = null
    override fun saveConfig(json: String) {}
    override fun fileDropTarget(onFiles: (List<String>) -> Unit): Modifier = Modifier
}

public actual fun platformServices(): PlatformServices = StubPlatform
