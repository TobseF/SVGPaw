package io.github.tobsef.svgokt.engine

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** A human readable file size like `243.1 KB`. */
public fun humanSize(bytes: Int?): String {
    if (bytes == null) return "–"
    var size = bytes.toDouble()
    for (unit in listOf("B", "KB", "MB", "GB")) {
        if (size < 1024 || unit == "GB") {
            return if (unit == "B") "${size.toInt()} B" else "${oneDecimal(size)} $unit"
        }
        size /= 1024
    }
    return "${oneDecimal(size)} GB"
}

/** Percentage saved going from [before] to [after] bytes. */
public fun savedPercent(before: Int, after: Int): Double =
    if (before == 0) 0.0 else (1 - after.toDouble() / before) * 100

/** `12.4` — Kotlin common has no printf, so the rounding is done by hand. */
public fun oneDecimal(value: Double): String {
    val scaled = (value * 10).roundToLong()
    val sign = if (scaled < 0) "-" else ""
    val magnitude = abs(scaled)
    return "$sign${magnitude / 10}.${magnitude % 10}"
}

/** A number without a trailing `.0`, matching Python's `%g` for the dimension readout. */
public fun trimNumber(value: Double): String {
    val rounded = value.roundToInt()
    return if (abs(value - rounded) < 1e-6) rounded.toString() else oneDecimal(value)
}

/** Shorten a long path to `…\parent\folder` so it fits in one label. */
public fun shortPath(path: String, keep: Int = 2, separator: Char = '/'): String {
    if (path.isEmpty()) return ""
    val parts = path.split('/', '\\').filter { it.isNotEmpty() }
    if (parts.size <= keep + 1) return path
    return "…" + separator + parts.takeLast(keep).joinToString(separator.toString())
}

private val WIDTH_RE = Regex("""\bwidth\s*=\s*"([^"]+)"""")
private val HEIGHT_RE = Regex("""\bheight\s*=\s*"([^"]+)"""")
private val VIEWBOX_RE = Regex("""\bviewBox\s*=\s*"([^"]+)"""")
private val NUM_RE = Regex("""-?[\d.]+""")

/** Best-effort width/height of an SVG document, mirroring SVGOMG's extractor. */
public fun svgDimensions(svg: String): Pair<Double, Double>? {
    val head = svg.take(4000)

    // `width="100%"` says nothing about how big the document is; the viewBox does, and returning
    // null here is what reaches it below.
    fun number(text: String): Double? {
        val match = NUM_RE.find(text) ?: return null
        if (text.substring(match.range.last + 1).trimStart().startsWith("%")) return null
        return match.value.toDoubleOrNull()
    }

    val w = WIDTH_RE.find(head)?.groupValues?.get(1)?.let(::number)
    val h = HEIGHT_RE.find(head)?.groupValues?.get(1)?.let(::number)
    if (w != null && h != null && w > 0 && h > 0) return w to h

    val viewBox = VIEWBOX_RE.find(head)?.groupValues?.get(1)?.trim()
    if (viewBox != null) {
        val parts = viewBox.split(Regex("[,\\s]+"))
        if (parts.size == 4) {
            val vw = parts[2].toDoubleOrNull()
            val vh = parts[3].toDoubleOrNull()
            if (vw != null && vh != null && vw > 0 && vh > 0) return vw to vh
        }
    }
    return null
}

/** Cheap sanity check before a file is accepted into the list. */
public fun looksLikeSvg(text: String): Boolean = text.take(5000).contains("<svg")
