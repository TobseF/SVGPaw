package io.github.tobsef.svgokt.model

import androidx.compose.ui.graphics.ImageBitmap

/** An opened SVG plus whatever has been computed from it so far. */
public data class SvgDoc(
    val path: String,
    val name: String,
    val original: String,
    val originalBytes: Int,
    val optimized: String? = null,
    val optimizedBytes: Int = 0,
    val error: String? = null,
    val originalImage: ImageBitmap? = null,
    val optimizedImage: ImageBitmap? = null,
    /** Settings fingerprint the current [optimized] was produced with. */
    val stamp: String? = null,
    /**
     * Settings fingerprint the preview bitmaps were rendered for. A failed render still sets this,
     * so it is never retried in a loop.
     */
    val renderStamp: String? = null,
    val originalGzip: Int? = null,
    val optimizedGzip: Int? = null,
) {
    public fun invalidated(): SvgDoc = copy(
        optimized = null,
        optimizedBytes = 0,
        optimizedImage = null,
        error = null,
        stamp = null,
        renderStamp = null,
        optimizedGzip = null,
    )

    /** Release the preview bitmaps; they are re-rendered when the document is selected again. */
    public fun withoutBitmaps(): SvgDoc =
        copy(originalImage = null, optimizedImage = null, renderStamp = null)
}
