@file:OptIn(ExperimentalComposeUiApi::class)

package io.github.tobsef.svgokt

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import io.github.tobsef.svgokt.model.SvgPawModel
import io.github.tobsef.svgokt.platform.PlatformServices
import io.github.tobsef.svgokt.platform.platformServices
import io.github.tobsef.svgokt.ui.SvgPawScreen
import io.github.tobsef.svgokt.ui.theme.SvgPawTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Regenerates the README screenshots by rendering the real Compose UI off-screen.
 *
 * Run it with `./gradlew :shared:jvmTest` (or from the IDE) whenever the UI changes and the images
 * in `docs/` need to catch up — every picture in the README is produced here, so there is one place
 * to keep them honest. Each case drives a live [SvgPawModel] exactly the way a click would, waits
 * for the optimize-and-rasterize pipeline to settle, and then paints one frame of [SvgPawScreen]
 * into an [ImageComposeScene] and writes it out as a PNG.
 *
 * The model is given a config-isolated platform so a run neither reads the developer's own settings
 * nor writes anything into `~/.svgpaw`; the screenshots therefore always show the shipped defaults.
 */
class ScreenshotTest {

    @Test
    fun generateReadmeScreenshots() {
        val root = repoRoot()
        val docs = File(root, "docs").apply { mkdirs() }
        // The first file is the one selected on load, so it is what the screenshots show.
        val samples = listOf("svg-cat.svg", "tiger.svg", "car.svg")
            .map { File(root, "sample/$it").absolutePath }

        // The hero: the whole window with a detailed drawing loaded and optimized.
        capture(File(docs, "screenshot.png"), samples)

        // The feature filter narrowed to "remove", the groups it hit expanded.
        capture(File(docs, "screenshot-features.png"), samples) { it.updateFeatureFilter("remove") }

        // The Markup tab, showing the optimized source that will be written.
        capture(File(docs, "screenshot-markup.png"), samples) { it.showMarkup(true) }

        // The output-settings dialog, over the main window.
        capture(
            File(docs, "screenshot-settings.png"),
            samples,
            width = SETTINGS_W,
            height = SETTINGS_H,
        ) { it.openSettingsDialog() }
    }

    /**
     * Load [files], wait until the selected one has been optimized and rasterized, apply [configure]
     * (a filter, a tab switch, a dialog…), then render the window once and save it to [out].
     */
    private fun capture(
        out: File,
        files: List<String>,
        width: Int = FULL_W,
        height: Int = FULL_H,
        configure: (SvgPawModel) -> Unit = {},
    ) {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        try {
            val model = SvgPawModel(IsolatedConfigPlatform(platformServices()), scope)
            runBlocking {
                model.openPaths(files)
                withTimeout(WAIT_MS) {
                    while (model.doc?.optimizedImage == null || model.working) delay(POLL_MS)
                }
                configure(model)
                // Let a filter change or a tab switch settle before it is painted.
                delay(POLL_MS)
            }

            val scene = ImageComposeScene(
                width = width,
                height = height,
                density = Density(DENSITY),
            ) {
                SvgPawTheme { SvgPawScreen(model) }
            }
            try {
                // The first frame lays everything out and runs the entry effects; the second paints
                // the settled result (image fit, list positions, the opened dialog).
                scene.render()
                val image = scene.render(FRAME_NANOS)
                val data = image.encodeToData(EncodedImageFormat.PNG)
                    ?: error("PNG encoding failed for ${out.name}")
                out.writeBytes(data.bytes)
            } finally {
                scene.close()
            }
        } finally {
            scope.cancel()
        }
        println("Wrote ${out.absolutePath}")
    }

    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir"))
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: error("Could not locate the repository root (no settings.gradle.kts found).")
    }

    private companion object {
        const val DENSITY = 1.5f

        // Logical window sizes (dp) times the density give the pixel dimensions written out.
        const val FULL_W = (1600 * 1.5f).toInt()
        const val FULL_H = (1000 * 1.5f).toInt()
        const val SETTINGS_W = (1280 * 1.5f).toInt()
        const val SETTINGS_H = (900 * 1.5f).toInt()

        const val WAIT_MS = 60_000L
        const val POLL_MS = 60L
        const val FRAME_NANOS = 16_000_000L
    }
}

/**
 * Wraps the real desktop platform but keeps config in memory: the screenshots use the shipped
 * defaults and the run never touches the developer's `~/.svgpaw`.
 */
private class IsolatedConfigPlatform(private val delegate: PlatformServices) :
    PlatformServices by delegate {
    override fun loadConfig(): String? = null
    override fun saveConfig(json: String) {}
}
