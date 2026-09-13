package io.github.tobsef.svgokt.engine

import io.github.tobsef.svgo.Config
import io.github.tobsef.svgo.Params
import io.github.tobsef.svgo.PluginConfig
import io.github.tobsef.svgo.StringifyOptions
import io.github.tobsef.svgo.VERSION
import io.github.tobsef.svgo.optimize
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.GZIPOutputStream

public actual fun optimizeSvg(svg: String, settings: SvgoSettings, path: String?): String {
    val plugins = settings.pluginRun().map { (id, params) -> PluginConfig(id, Params(params)) }
    val config = Config(
        plugins = plugins,
        multipass = settings.multipass,
        path = path,
        js2svg = StringifyOptions(indent = settings.indent, pretty = settings.pretty),
    )
    return try {
        optimize(svg, config).data
    } catch (e: Exception) {
        throw SvgoException("${e::class.simpleName}: ${e.message}", e)
    }
}

public actual fun svgoVersion(): String = VERSION

public actual fun gzipSize(data: ByteArray): Int {
    val sink = ByteArrayOutputStream(data.size / 3 + 64)
    // GZIPOutputStream defaults to level 6; SVGOMG compares at maximum compression.
    MaxCompressionGzipStream(sink).use { it.write(data) }
    return sink.size()
}

private class MaxCompressionGzipStream(sink: java.io.OutputStream) : GZIPOutputStream(sink) {
    init {
        def.setLevel(Deflater.BEST_COMPRESSION)
    }
}
