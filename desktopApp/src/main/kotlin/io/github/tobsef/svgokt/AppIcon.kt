package io.github.tobsef.svgokt

import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface
import org.jetbrains.skia.svg.SVGDOM
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * The window and task-bar icon, rendered from the app's own SVG.
 *
 * Windows picks whichever size fits the slot it is filling — 16 px in the title bar, 32 in the
 * task bar, 256 in Alt+Tab — so it gets the whole ladder rather than one bitmap to resample. The
 * app already carries an SVG renderer, so each rung is drawn at its exact size instead of being
 * scaled down from a fixed PNG.
 */
private val ICON_SIZES = listOf(16, 20, 24, 32, 40, 48, 64, 128, 256)

fun loadAppIcons(): List<BufferedImage> {
    val svg = AppIconMarker::class.java.getResourceAsStream("/icon.svg")
        ?.use { it.readBytes() }
        ?: return emptyList()

    return ICON_SIZES.mapNotNull { size -> renderPng(svg, size) }
}

private fun renderPng(svg: ByteArray, size: Int): BufferedImage? = runCatching {
    val dom = SVGDOM(Data.makeFromBytes(svg))
    val surface = Surface.makeRasterN32Premul(size, size)
    // The document is a square sized in percent, so the container size is all it needs.
    dom.setContainerSize(size.toFloat(), size.toFloat())
    dom.render(surface.canvas)

    val png = surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)?.bytes
        ?: return@runCatching null
    ImageIO.read(png.inputStream())
}.getOrNull()

/** Only here to give [loadAppIcons] a class to resolve the resource against. */
private class AppIconMarker
