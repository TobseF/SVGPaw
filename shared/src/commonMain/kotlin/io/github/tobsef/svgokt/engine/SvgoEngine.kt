package io.github.tobsef.svgokt.engine

/** Raised when SVGO could not optimize a document. */
public class SvgoException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * The one call the UI makes into svgo-kt.
 *
 * It is an `expect` because the library is published for jvm/js/native but not for wasmJs, and
 * because the first iteration only has to work on the desktop. Non-JVM actuals are stubs.
 *
 * @param path only passed through to SVGO so plugins that look at the file name (`prefixIds`)
 *   behave the way the CLI would.
 */
public expect fun optimizeSvg(svg: String, settings: SvgoSettings, path: String? = null): String

/** The SVGO version svgo-kt was ported from — shown in the sidebar's subtitle. */
public expect fun svgoVersion(): String

/** Size of [data] after gzip -9, the way SVGOMG's "compare gzipped" does it. */
public expect fun gzipSize(data: ByteArray): Int

/**
 * The plugin list an SVGO run is configured with, shared by every platform's actual.
 *
 * Enabled plugins are listed individually rather than through `preset-default` + overrides, so the
 * UI's on/off state maps one to one onto what runs, in catalogue order.
 */
public fun SvgoSettings.pluginRun(): List<Pair<String, Map<String, Any?>>> =
    PLUGIN_IDS.filter { isEnabled(it) }.map { id ->
        // A precision of 0 almost always breaks the image when applied to cleanupNumericValues,
        // so SVGOMG bumps that one plugin to 1.
        val precision = if (id == "cleanupNumericValues" && floatPrecision == 0) 1 else floatPrecision
        id to mapOf<String, Any?>(
            "floatPrecision" to precision,
            "transformPrecision" to transformPrecision,
        )
    }
