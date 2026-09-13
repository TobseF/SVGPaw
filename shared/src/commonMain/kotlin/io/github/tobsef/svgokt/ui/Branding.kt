package io.github.tobsef.svgokt.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.github.tobsef.svgokt.platform.PlatformServices

public const val APP_NAME: String = "SVGPaw"
public const val APP_SLOGAN: String = "Minimize your SVG footprint"

/**
 * The in-app artwork is kept as SVG source and rasterized through the same renderer the preview
 * uses, so it stays crisp at any size or DPI and needs no resource pipeline of its own.
 */
public const val PAW_SVG: String =
    """<svg xmlns="http://www.w3.org/2000/svg" width="641" height="597" viewBox="383 428 1282 1194">""" +
        """<g fill="#e23744" transform="translate(-122.88 -123.12)scale(1.12)">""" +
        """<path d="M1024 990c156 0 276 140 366 280 80 130 50 270-80 280-100 8-190-50-286-50s-186 58-286 50c-130-10-160-150-80-280 90-140 210-280 366-280"/>""" +
        """<ellipse cx="590" cy="885" rx="118" ry="158" transform="rotate(-32 590 885)"/>""" +
        """<ellipse cx="848" cy="670" rx="128" ry="172" transform="rotate(-10 848 670)"/>""" +
        """<ellipse cx="1200" cy="670" rx="128" ry="172" transform="rotate(10 1200 670)"/>""" +
        """<ellipse cx="1458" cy="885" rx="118" ry="158" transform="rotate(32 1458 885)"/>""" +
        """</g></svg>"""

private const val GEAR_SVG: String =
    """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 39 40" width="39" height="40">""" +
        """<path fill="COLOR" fill-rule="nonzero" d="M16.077,0c-0.958,0 -1.777,0.677 -1.961,1.617l-0.668,3.43c-1.642,0.621 -3.159,1.488 -4.492,2.582l-3.289,-1.133c-0.906,-0.312 -1.903,0.063 -2.383,0.891l-3.016,5.226c-0.478,0.83 -0.304,1.884 0.418,2.512l2.637,2.293c-0.137,0.843 -0.227,1.701 -0.227,2.582c0,0.881 0.09,1.739 0.227,2.582l-2.637,2.293c-0.722,0.628 -0.896,1.682 -0.418,2.512l3.016,5.226c0.478,0.83 1.477,1.205 2.383,0.895l3.289,-1.133c1.332,1.093 2.851,1.958 4.492,2.578l0.668,3.43c0.184,0.94 1.003,1.617 1.961,1.617l6.039,0c0.958,0 1.777,-0.677 1.961,-1.617l0.668,-3.43c1.641,-0.621 3.159,-1.488 4.492,-2.582l3.289,1.133c0.906,0.312 1.903,-0.061 2.383,-0.891l3.015,-5.23c0.478,-0.83 0.304,-1.88 -0.418,-2.508l-2.636,-2.293c0.137,-0.843 0.226,-1.701 0.226,-2.582c0,-0.881 -0.089,-1.739 -0.226,-2.582l2.636,-2.293c0.722,-0.628 0.896,-1.682 0.418,-2.512l-3.015,-5.226c-0.478,-0.83 -1.477,-1.205 -2.383,-0.895l-3.289,1.133c-1.332,-1.093 -2.851,-1.958 -4.492,-2.578l-0.668,-3.43c-0.184,-0.94 -1.003,-1.617 -1.961,-1.617l-6.039,-0Zm3.019,12c4.418,-0 8,3.582 8,8c0,4.418 -3.582,8 -8,8c-4.418,-0 -8,-3.582 -8,-8c0,-4.418 3.582,-8 8,-8Z"/>""" +
        """</svg>"""

@Composable
public fun PawLogo(platform: PlatformServices, size: Int = 30, modifier: Modifier = Modifier) {
    val bitmap = remember(size) { platform.rasterize(PAW_SVG, size * 3) }
    if (bitmap == null) {
        Box(modifier.size(size.dp))
    } else {
        Image(bitmap, contentDescription = APP_NAME, modifier = modifier.size(size.dp), contentScale = ContentScale.Fit)
    }
}

@Composable
public fun GearIcon(platform: PlatformServices, tint: Color, size: Int = 18, modifier: Modifier = Modifier) {
    val hex = tint.toHex()
    val bitmap = remember(hex, size) { platform.rasterize(GEAR_SVG.replace("COLOR", hex), size * 4) }
    if (bitmap == null) {
        Box(modifier.size(size.dp))
    } else {
        Image(bitmap, contentDescription = "Settings", modifier = modifier.size(size.dp), contentScale = ContentScale.Fit)
    }
}

private fun Color.toHex(): String {
    fun channel(value: Float) = ((value * 255).toInt().coerceIn(0, 255)).toString(16).padStart(2, '0')
    return "#${channel(red)}${channel(green)}${channel(blue)}"
}
