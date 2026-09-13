package io.github.tobsef.svgokt.model

import io.github.tobsef.svgokt.engine.SvgoSettings
import io.github.tobsef.svgokt.platform.PlatformServices
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Everything that survives a restart: where files go, how they are named, and the SVGO settings. */
@Serializable
public data class AppConfig(
    val useSourceDir: Boolean = true,
    val outputDir: String = "",
    val prefix: String = "min_",
    val suffix: String = "",
    val overwrite: Boolean = false,
    val compareGzip: Boolean = true,
    val bgMode: String = BackdropMode.CHECKER.key,
    val collapsedGroups: List<String> = emptyList(),
    val lastOpenDir: String = "",
    val svgoSettings: SvgoSettings = SvgoSettings.DEFAULT,
)

/** The backdrop the preview image is drawn on. */
public enum class BackdropMode(public val key: String, public val label: String) {
    CHECKER("checker", "Checkerboard"),
    WHITE("white", "White"),
    BLACK("black", "Black"),
    DARK("dark", "Dark");

    public companion object {
        public fun of(key: String?): BackdropMode = entries.firstOrNull { it.key == key } ?: CHECKER
    }
}

/** Reads and writes [AppConfig]; any corruption falls back to the defaults. */
public class ConfigStore(private val platform: PlatformServices) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    public fun load(): AppConfig {
        val raw = platform.loadConfig() ?: return defaults()
        return try {
            json.decodeFromString<AppConfig>(raw).let {
                it.copy(
                    svgoSettings = it.svgoSettings.normalized(),
                    outputDir = it.outputDir.ifEmpty { defaultOutputDir() },
                )
            }
        } catch (_: Exception) {
            defaults()
        }
    }

    public fun save(config: AppConfig) {
        try {
            platform.saveConfig(json.encodeToString(config))
        } catch (_: Exception) {
            // A settings file we cannot write is not worth interrupting the user over.
        }
    }

    private fun defaults() = AppConfig(outputDir = defaultOutputDir())

    private fun defaultOutputDir() = platform.join(platform.homeDir(), "Downloads")
}
