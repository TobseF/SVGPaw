package io.github.tobsef.svgokt.engine

import kotlinx.serialization.Serializable

/**
 * Everything an SVGO run is configured with: the globals plus one flag per plugin.
 *
 * Defaults come from SVGOMG's `index.html` for the globals and from SVGO's `preset-default` for
 * the plugin flags.
 */
@Serializable
public data class SvgoSettings(
    val floatPrecision: Int = 3,
    val transformPrecision: Int = 5,
    val multipass: Boolean = false,
    val pretty: Boolean = false,
    val indent: Int = 2,
    val plugins: Map<String, Boolean> = PLUGINS.associate { it.id to it.defaultEnabled },
) {
    public fun isEnabled(id: String): Boolean = plugins[id] ?: false

    public fun withPlugin(id: String, enabled: Boolean): SvgoSettings =
        copy(plugins = plugins + (id to enabled))

    /**
     * Fill in missing plugin ids and drop unknown ones, so a config written by an older build
     * stays usable.
     */
    public fun normalized(): SvgoSettings = copy(
        floatPrecision = floatPrecision.coerceIn(PRECISION_MIN, PRECISION_MAX),
        transformPrecision = transformPrecision.coerceIn(PRECISION_MIN, PRECISION_MAX),
        plugins = PLUGINS.associate { it.id to (plugins[it.id] ?: it.defaultEnabled) },
    )

    /** How many plugins of [category] are on, and how many there are. */
    public fun countFor(members: List<PluginInfo>): Pair<Int, Int> =
        members.count { isEnabled(it.id) } to members.size

    public val enabledCount: Int get() = PLUGINS.count { isEnabled(it.id) }

    public companion object {
        public val DEFAULT: SvgoSettings = SvgoSettings()
    }
}

public const val PRECISION_MIN: Int = 0
public const val PRECISION_MAX: Int = 8

/**
 * A stable fingerprint of the settings. Two documents optimized under the same fingerprint are
 * guaranteed to be identical, which is what lets the app skip re-running finished work.
 */
public fun SvgoSettings.stamp(): String = buildString {
    append(floatPrecision).append('|')
    append(transformPrecision).append('|')
    append(multipass).append('|')
    append(pretty).append('|')
    append(indent).append('|')
    // Catalogue order, so the string is independent of the map's iteration order.
    for (id in PLUGIN_IDS) append(if (isEnabled(id)) '1' else '0')
}
