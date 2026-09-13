package io.github.tobsef.svgokt.engine

/**
 * Android stub. The first iteration ships the desktop app only; svgo-kt is wired into `jvmMain`
 * and this target gets it when its own front-end is built.
 */
public actual fun optimizeSvg(svg: String, settings: SvgoSettings, path: String?): String =
    throw SvgoException("SVG optimization is not wired up on Android yet.")

public actual fun svgoVersion(): String = "4.1.0"

public actual fun gzipSize(data: ByteArray): Int = data.size
