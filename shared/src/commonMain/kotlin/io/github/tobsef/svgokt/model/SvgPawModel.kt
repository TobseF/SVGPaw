package io.github.tobsef.svgokt.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import io.github.tobsef.svgokt.engine.PLUGINS
import io.github.tobsef.svgokt.engine.SvgoSettings
import io.github.tobsef.svgokt.engine.gzipSize
import io.github.tobsef.svgokt.engine.humanSize
import io.github.tobsef.svgokt.engine.looksLikeSvg
import io.github.tobsef.svgokt.engine.oneDecimal
import io.github.tobsef.svgokt.engine.optimizeSvg
import io.github.tobsef.svgokt.engine.savedPercent
import io.github.tobsef.svgokt.engine.shortPath
import io.github.tobsef.svgokt.engine.stamp
import io.github.tobsef.svgokt.platform.PlatformServices
import io.github.tobsef.svgokt.platform.RENDER_SIZE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

/** How the status line is tinted. */
public enum class Tone { Normal, Good, Bad }

public data class StatusMessage(val text: String, val tone: Tone = Tone.Normal)

/**
 * Everything the window shows, and the one place work is started from.
 *
 * ### Concurrency
 * Optimizing is CPU-bound and can take seconds on a large drawing, so it never runs on the UI
 * thread. Every change that invalidates the preview — a plugin toggle, a precision slider, a
 * different file — pushes a [Request] into [requests]; a single collector picks it up with
 * `collectLatest`, which cancels whatever it was doing the moment a newer request arrives. A
 * settings change additionally waits out a short debounce inside that cancellable window, so
 * dragging a slider queues exactly one run rather than one per pixel.
 *
 * `optimize` itself is a plain blocking call that cannot be interrupted mid-run, so a superseded
 * job finishes its current pass before it notices; its result is then dropped at the [ensureActive]
 * checkpoints instead of reaching the screen. Running on a bounded worker dispatcher keeps those
 * stragglers from saturating the machine.
 */
public class SvgPawModel(
    public val platform: PlatformServices,
    private val scope: CoroutineScope,
) {
    private val store = ConfigStore(platform)

    // ---- persisted state --------------------------------------------------- #

    public var config: AppConfig by mutableStateOf(store.load())
        private set

    public var settings: SvgoSettings by mutableStateOf(config.svgoSettings)
        private set

    // ---- documents --------------------------------------------------------- #

    public var docs: List<SvgDoc> by mutableStateOf(emptyList())
        private set

    public var current: Int by mutableStateOf(-1)
        private set

    public val doc: SvgDoc? get() = docs.getOrNull(current)

    // ---- view state -------------------------------------------------------- #

    public var showingOptimized: Boolean by mutableStateOf(true)
        private set

    public var markupMode: Boolean by mutableStateOf(false)
        private set

    public var featureFilter: String by mutableStateOf("")
        private set

    public var collapsedGroups: Set<String> by mutableStateOf(config.collapsedGroups.toSet())
        private set

    public var settingsDialogOpen: Boolean by mutableStateOf(false)
        private set

    /**
     * A text field has the caret. Single-key shortcuts stand down while it does, so typing a
     * space into the filter box does not flip the preview.
     */
    public var textInputFocused: Boolean by mutableStateOf(false)
        private set

    public var status: StatusMessage by mutableStateOf(StatusMessage("Ready."))
        private set

    /** A preview job is in flight. */
    public var working: Boolean by mutableStateOf(false)
        private set

    /** A "Save all" run is in flight; it locks the action buttons. */
    public var busy: Boolean by mutableStateOf(false)
        private set

    /** `null` while nothing is being written, otherwise 0..1. */
    public var saveProgress: Float? by mutableStateOf(null)
        private set

    public val backdrop: BackdropMode get() = BackdropMode.of(config.bgMode)

    public val compareGzip: Boolean get() = config.compareGzip

    /** Bumped whenever the viewer should snap back to "Fit". */
    public var fitRequest: Int by mutableStateOf(0)
        private set

    // ---- the job pipeline --------------------------------------------------- #

    private data class Request(
        val seq: Long,
        val path: String?,
        val stamp: String,
        val debounce: Boolean,
    )

    private val requests = MutableStateFlow<Request?>(null)
    private var seq = 0L

    /** Bounded so superseded, uninterruptible runs cannot take the whole machine with them. */
    private val worker = Dispatchers.Default.limitedParallelism(2)

    init {
        scope.launch {
            requests.collectLatest { request ->
                if (request == null) return@collectLatest
                if (request.debounce) delay(DEBOUNCE_MS)
                runJob(request)
            }
        }
    }

    private fun requestRefresh(debounce: Boolean) {
        val target = doc
        if (target == null) {
            working = false
            requests.value = null
            return
        }
        // Already done under exactly these settings, bitmaps included: nothing to run.
        val stamp = settings.stamp()
        if (target.stamp == stamp && target.renderStamp == stamp) {
            working = false
            requests.value = null
            return
        }
        working = true
        status = StatusMessage("Optimizing ${target.name}…")
        requests.value = Request(++seq, target.path, stamp, debounce)
    }

    private suspend fun runJob(request: Request) {
        val target = docs.firstOrNull { it.path == request.path } ?: return
        val snapshot = settings
        val needOriginal = target.originalImage == null

        val result = withContext(worker) {
            var optimized: String? = null
            var error: String? = null
            try {
                optimized = optimizeSvg(target.original, snapshot, target.path)
            } catch (e: Exception) {
                error = e.message ?: e.toString()
            }
            coroutineContext.ensureActive()

            val originalImage = if (needOriginal) {
                platform.rasterize(target.original, RENDER_SIZE)
            } else {
                null
            }
            coroutineContext.ensureActive()

            val optimizedImage = optimized?.let { platform.rasterize(it, RENDER_SIZE) }
            coroutineContext.ensureActive()

            val optimizedBytes = optimized?.encodeToByteArray()
            JobResult(
                optimized = optimized,
                optimizedBytes = optimizedBytes?.size ?: 0,
                optimizedGzip = optimizedBytes?.let { gzipSize(it) },
                originalGzip = target.originalGzip
                    ?: gzipSize(target.original.encodeToByteArray()),
                error = error,
                originalImage = originalImage,
                optimizedImage = optimizedImage,
            )
        }
        coroutineContext.ensureActive()
        applyResult(request, result)
    }

    private class JobResult(
        val optimized: String?,
        val optimizedBytes: Int,
        val optimizedGzip: Int?,
        val originalGzip: Int,
        val error: String?,
        val originalImage: ImageBitmap?,
        val optimizedImage: ImageBitmap?,
    )

    private fun applyResult(request: Request, result: JobResult) {
        docs = docs.map { candidate ->
            if (candidate.path != request.path) {
                candidate
            } else {
                candidate.copy(
                    optimized = result.optimized,
                    optimizedBytes = result.optimizedBytes,
                    optimizedGzip = result.optimizedGzip,
                    originalGzip = result.originalGzip,
                    error = result.error,
                    originalImage = result.originalImage ?: candidate.originalImage,
                    optimizedImage = result.optimizedImage,
                    stamp = request.stamp,
                    renderStamp = request.stamp,
                )
            }
        }
        working = false

        val updated = docs.firstOrNull { it.path == request.path } ?: return
        status = if (result.error != null) {
            StatusMessage("SVGO error: ${result.error}", Tone.Bad)
        } else {
            val saved = savedPercent(updated.originalBytes, updated.optimizedBytes)
            val verb = if (saved >= 0) "smaller" else "larger"
            StatusMessage(
                "${updated.name} · ${humanSize(updated.optimizedBytes)} · " +
                    "${oneDecimal(abs(saved))}% $verb than the original",
            )
        }
    }

    // ---- files -------------------------------------------------------------- #

    public fun chooseFiles() {
        scope.launch {
            val start = config.lastOpenDir.ifEmpty { platform.homeDir() }
            val picked = platform.pickSvgFiles(start)
            if (picked.isNotEmpty()) openPaths(picked)
        }
    }

    public fun openPaths(paths: List<String>) {
        scope.launch {
            var added = 0
            val skipped = mutableListOf<String>()
            var firstIndex: Int? = null
            var list = docs

            for (raw in paths) {
                val candidates = platform.listSvgFiles(raw) ?: listOf(raw)
                for (candidate in candidates) {
                    if (!candidate.lowercase().let { it.endsWith(".svg") || it.endsWith(".svgz") }) {
                        skipped += platform.fileName(candidate)
                        continue
                    }
                    val text = try {
                        platform.readSvg(candidate)
                    } catch (e: Exception) {
                        platform.alert("SVGPaw", "Could not read:\n$candidate\n\n${e.message}")
                        continue
                    }
                    if (!looksLikeSvg(text)) {
                        platform.alert("SVGPaw", "Not an SVG document:\n$candidate")
                        continue
                    }
                    val existing = list.indexOfFirst { it.path == candidate }
                    val fresh = SvgDoc(
                        path = candidate,
                        name = platform.fileName(candidate),
                        original = text,
                        originalBytes = text.encodeToByteArray().size,
                    )
                    if (existing >= 0) {
                        // Re-opened -> refresh it in place.
                        list = list.toMutableList().also { it[existing] = fresh }
                        if (firstIndex == null) firstIndex = existing
                    } else {
                        list = list + fresh
                        if (firstIndex == null) firstIndex = list.lastIndex
                    }
                    added++
                }
            }

            docs = list
            if (firstIndex != null) {
                val first = paths.first()
                updateConfig {
                    it.copy(
                        lastOpenDir = if (platform.listSvgFiles(first) != null) {
                            first
                        } else {
                            platform.parentDir(first)
                        },
                    )
                }
                select(firstIndex)
                status = StatusMessage("Opened $added ${if (added == 1) "file" else "files"}.")
            }
            if (skipped.isNotEmpty()) {
                val shown = skipped.take(3).joinToString(", ") + if (skipped.size > 3) "…" else ""
                status = StatusMessage(
                    "Ignored ${skipped.size} non-SVG file(s): $shown",
                    Tone.Bad,
                )
            }
        }
    }

    public fun select(index: Int) {
        if (index !in docs.indices) return
        current = index
        // Preview bitmaps are large; only the selected document keeps its own.
        docs = docs.mapIndexed { i, d -> if (i == index) d else d.withoutBitmaps() }
        fitRequest++
        requestRefresh(debounce = false)
    }

    public fun removeDoc(index: Int) {
        if (index !in docs.indices) return
        docs = docs.filterIndexed { i, _ -> i != index }
        current = if (docs.isEmpty()) -1 else current.coerceAtMost(docs.lastIndex)
        requestRefresh(debounce = false)
    }

    public fun clearFiles() {
        docs = emptyList()
        current = -1
        requests.value = null
        working = false
        status = StatusMessage("Ready.")
    }

    // ---- settings ------------------------------------------------------------ #

    private fun onSettingsChanged(next: SvgoSettings) {
        settings = next
        // Every document's result is stale now; the selected one is recomputed right away, the
        // others lazily when they are selected.
        docs = docs.map { it.invalidated() }
        updateConfig(persist = true) { it.copy(svgoSettings = next) }
        requestRefresh(debounce = true)
    }

    public fun setMultipass(value: Boolean): Unit = onSettingsChanged(settings.copy(multipass = value))

    public fun setPretty(value: Boolean): Unit = onSettingsChanged(settings.copy(pretty = value))

    public fun setFloatPrecision(value: Int) {
        if (value != settings.floatPrecision) onSettingsChanged(settings.copy(floatPrecision = value))
    }

    public fun setTransformPrecision(value: Int) {
        if (value != settings.transformPrecision) {
            onSettingsChanged(settings.copy(transformPrecision = value))
        }
    }

    public fun setPlugin(id: String, enabled: Boolean): Unit =
        onSettingsChanged(settings.withPlugin(id, enabled))

    public fun resetSettings(): Unit = onSettingsChanged(SvgoSettings.DEFAULT)

    public fun setCompareGzip(value: Boolean) {
        updateConfig(persist = true) { it.copy(compareGzip = value) }
    }

    public fun setBackdrop(mode: BackdropMode) {
        updateConfig(persist = true) { it.copy(bgMode = mode.key) }
    }

    public fun updateFeatureFilter(value: String) {
        featureFilter = value
    }

    public fun onTextInputFocus(focused: Boolean) {
        textInputFocused = focused
    }

    public fun toggleGroup(key: String) {
        collapsedGroups = if (key in collapsedGroups) collapsedGroups - key else collapsedGroups + key
        persistGroups()
    }

    public fun toggleAllGroups() {
        val keys = io.github.tobsef.svgokt.engine.PLUGINS_BY_CATEGORY.map { it.first.key }
        collapsedGroups = if (collapsedGroups.size == keys.size) emptySet() else keys.toSet()
        persistGroups()
    }

    public val allCollapsed: Boolean
        get() = collapsedGroups.size == io.github.tobsef.svgokt.engine.PLUGINS_BY_CATEGORY.size

    private fun persistGroups() {
        updateConfig(persist = true) { it.copy(collapsedGroups = collapsedGroups.toList()) }
    }

    // ---- view toggles --------------------------------------------------------- #

    public fun showOptimized(value: Boolean) {
        showingOptimized = value
    }

    public fun toggleShowing() {
        showingOptimized = !showingOptimized
    }

    public fun showMarkup(value: Boolean) {
        markupMode = value
    }

    public fun requestFit() {
        fitRequest++
    }

    public fun copyMarkup() {
        val target = doc ?: return
        val text = if (showingOptimized && target.optimized != null) {
            target.optimized
        } else {
            target.original
        }
        platform.copyToClipboard(text ?: return)
        status = StatusMessage("Markup copied to the clipboard.")
    }

    // ---- saving ---------------------------------------------------------------- #

    /** Where [target] would be written given the current output settings. */
    public fun targetPath(target: SvgDoc): String {
        val name = "${config.prefix}${platform.baseName(target.path)}${config.suffix}.svg"
        val folder = if (config.useSourceDir) {
            platform.parentDir(target.path)
        } else {
            config.outputDir.ifEmpty { platform.parentDir(target.path) }
        }
        return platform.join(folder, name)
    }

    /** The "Saves as min_<name>.svg in …" hint next to the action buttons. */
    public fun targetHint(): String {
        val where = if (config.useSourceDir) {
            "the source folder"
        } else {
            shortPath(config.outputDir).ifEmpty { "the source folder" }
        }
        val pattern = "${config.prefix}<name>${config.suffix}.svg"
        val overwrite = if (config.overwrite) " · overwrites silently" else ""
        return "Saves as  $pattern\nin $where$overwrite"
    }

    public fun saveCurrent() {
        val target = doc ?: return
        val data = target.optimized ?: return
        if (busy) return
        scope.launch {
            val path = targetPath(target)
            if (!confirmOverwrite(path)) {
                status = StatusMessage("Save cancelled.")
                return@launch
            }
            try {
                platform.writeFile(path, data)
            } catch (e: Exception) {
                platform.alert("SVGPaw", "Could not save:\n$path\n\n${e.message}")
                return@launch
            }
            val saved = savedPercent(target.originalBytes, target.optimizedBytes)
            status = StatusMessage(
                "Saved ${platform.fileName(path)} · ${humanSize(target.optimizedBytes)} " +
                    "(${oneDecimal(saved)}% smaller) → ${shortPath(platform.parentDir(path), 3)}",
                Tone.Good,
            )
        }
    }

    public fun saveAs() {
        val target = doc ?: return
        val data = target.optimized ?: return
        scope.launch {
            val suggested = targetPath(target)
            val chosen = platform.pickSaveTarget(
                platform.fileName(suggested),
                platform.parentDir(suggested),
            ) ?: return@launch
            try {
                platform.writeFile(chosen, data)
            } catch (e: Exception) {
                platform.alert("SVGPaw", "Could not save:\n$chosen\n\n${e.message}")
                return@launch
            }
            status = StatusMessage("Saved ${platform.fileName(chosen)}", Tone.Good)
        }
    }

    public fun saveAll() {
        if (docs.isEmpty() || busy) return
        scope.launch {
            val targets = docs.map { it to targetPath(it) }
            val clashes = targets.map { it.second }.filter { platform.fileExists(it) }
            if (clashes.isNotEmpty() && !config.overwrite) {
                val listed = clashes.take(8).joinToString("\n") { "• ${platform.fileName(it)}" }
                val more = if (clashes.size > 8) "\n… and ${clashes.size - 8} more" else ""
                val go = platform.confirm(
                    "SVGPaw",
                    "${clashes.size} file(s) already exist and will be replaced:\n\n" +
                        "$listed$more\n\nContinue?",
                )
                if (!go) {
                    status = StatusMessage("Save all cancelled.")
                    return@launch
                }
            }

            busy = true
            saveProgress = 0f
            val snapshot = settings
            val snapshotStamp = snapshot.stamp()
            var written = 0
            var beforeTotal = 0
            var afterTotal = 0
            val failures = mutableListOf<String>()

            targets.forEachIndexed { i, (target, path) ->
                status = StatusMessage("Optimizing ${target.name}  (${i + 1}/${targets.size})…")
                try {
                    val data = if (target.optimized != null && target.stamp == snapshotStamp) {
                        target.optimized
                    } else {
                        withContext(worker) { optimizeSvg(target.original, snapshot, target.path) }
                    }
                    platform.writeFile(path, data)
                    written++
                    beforeTotal += target.originalBytes
                    afterTotal += data.encodeToByteArray().size
                } catch (e: Exception) {
                    failures += "${target.name}: ${e.message}"
                }
                saveProgress = (i + 1).toFloat() / targets.size
            }

            busy = false
            saveProgress = null
            val noun = if (written == 1) "file" else "files"
            if (failures.isNotEmpty()) {
                status = StatusMessage("Saved $written $noun, ${failures.size} failed.", Tone.Bad)
                platform.alert(
                    "SVGPaw",
                    "Some files could not be saved:\n\n" + failures.take(10).joinToString("\n"),
                )
            } else {
                status = StatusMessage(
                    "Saved $written $noun · ${humanSize(beforeTotal)} → ${humanSize(afterTotal)} " +
                        "(${oneDecimal(savedPercent(beforeTotal, afterTotal))}% smaller)",
                    Tone.Good,
                )
            }
            // Re-run the selected document so the stats match what was written.
            requestRefresh(debounce = false)
        }
    }

    private suspend fun confirmOverwrite(path: String): Boolean {
        if (!platform.fileExists(path) || config.overwrite) return true
        return platform.confirm(
            "SVGPaw",
            "${platform.fileName(path)} already exists in\n${platform.parentDir(path)}\n\nReplace it?",
        )
    }

    // ---- output settings dialog ------------------------------------------------- #

    public fun openSettingsDialog() {
        settingsDialogOpen = true
    }

    public fun closeSettingsDialog() {
        settingsDialogOpen = false
    }

    public fun applyOutputSettings(
        useSourceDir: Boolean,
        outputDir: String,
        prefix: String,
        suffix: String,
        overwrite: Boolean,
    ) {
        updateConfig(persist = true) {
            it.copy(
                useSourceDir = useSourceDir,
                outputDir = outputDir.trim(),
                prefix = prefix.trim(),
                suffix = suffix.trim(),
                overwrite = overwrite,
            )
        }
        settingsDialogOpen = false
    }

    public fun browseOutputDir(startDir: String, onPicked: (String) -> Unit) {
        scope.launch { platform.pickDirectory(startDir.ifEmpty { platform.homeDir() })?.let(onPicked) }
    }

    // ---- derived readouts --------------------------------------------------------- #

    /** Sizes for the stats row, honouring the "compare gzipped" toggle. */
    public fun statSizes(target: SvgDoc): Pair<Int, Int?> {
        val before = if (compareGzip) target.originalGzip ?: target.originalBytes else target.originalBytes
        val after = when {
            target.optimized == null -> null
            compareGzip -> target.optimizedGzip ?: target.optimizedBytes
            else -> target.optimizedBytes
        }
        return before to after
    }

    public val featuresSummary: String
        get() {
            val needle = featureFilter.trim().lowercase()
            if (needle.isEmpty()) {
                return "${settings.enabledCount} of ${PLUGINS.size} features enabled"
            }
            val matches = PLUGINS.count { it.matches(needle) }
            return "$matches ${if (matches == 1) "feature" else "features"} match “$needle”."
        }

    private fun updateConfig(persist: Boolean = true, block: (AppConfig) -> AppConfig) {
        config = block(config)
        if (persist) store.save(config)
    }

    private companion object {
        /** Long enough that dragging a slider queues one run, short enough to feel live. */
        const val DEBOUNCE_MS = 260L
    }
}

/** Does a plugin match the features filter? Label and id are both searchable. */
public fun io.github.tobsef.svgokt.engine.PluginInfo.matches(needle: String): Boolean =
    needle.isEmpty() || label.lowercase().contains(needle) || id.lowercase().contains(needle)
