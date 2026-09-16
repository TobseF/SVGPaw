package io.github.tobsef.svgokt.engine

import io.github.tobsef.svgo.Config
import io.github.tobsef.svgo.Params
import io.github.tobsef.svgo.PluginConfig
import io.github.tobsef.svgo.StringifyOptions
import io.github.tobsef.svgo.VERSION
import io.github.tobsef.svgo.optimize

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

// The browser has no gzip API this build reaches for; the raw size is still an honest, if
// pessimistic, stand-in until that's wired up too.
public actual fun gzipSize(data: ByteArray): Int = data.size
