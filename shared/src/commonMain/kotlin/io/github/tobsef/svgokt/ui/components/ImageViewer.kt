package io.github.tobsef.svgokt.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.tobsef.svgokt.model.BackdropMode
import io.github.tobsef.svgokt.ui.theme.Palette
import kotlin.math.roundToInt

private val CHECKER_A = Color(0xFF2A2A2A)
private val CHECKER_B = Color(0xFF353535)
private const val CHECKER_SIZE = 16f
private const val MIN_ZOOM = 0.05f
private const val MAX_ZOOM = 40f

/** A zoom-button press. [seq] distinguishes repeated presses of the same button. */
public data class ZoomCommand(val seq: Int, val factor: Float)

/**
 * The preview stage: a rasterized SVG that can be panned and zoomed over a choice of backdrops.
 *
 * Zoom is stored as a multiplier over "fit", so a new document or a window resize keeps whatever
 * magnification the user had picked rather than jumping.
 *
 * @param fitRequest bump this to snap back to Fit.
 * @param zoomCommand a zoom-button press; the sequence number is what makes a repeated press of
 *   the same button register as a new command.
 */
@Composable
public fun ImageViewer(
    image: ImageBitmap?,
    placeholder: String,
    backdrop: BackdropMode,
    fitRequest: Int,
    modifier: Modifier = Modifier,
    zoomCommand: ZoomCommand? = null,
) {
    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(fitRequest) {
        zoom = 1f
        pan = Offset.Zero
    }
    LaunchedEffect(zoomCommand?.seq) {
        if (zoomCommand != null) zoom = (zoom * zoomCommand.factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        pan += dragAmount
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        zoom = 1f
                        pan = Offset.Zero
                    })
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type != PointerEventType.Scroll) continue
                            val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                            if (delta == 0f) continue
                            // Scroll up (negative) magnifies, the way every image viewer does it.
                            zoom = (zoom * if (delta < 0) 1.15f else 1 / 1.15f)
                                .coerceIn(MIN_ZOOM, MAX_ZOOM)
                            event.changes.forEach { it.consume() }
                        }
                    }
                },
        ) {
            drawBackdrop(backdrop)
            if (image == null) return@Canvas

            val fit = minOf(size.width / image.width, size.height / image.height)
            val scale = fit * zoom
            val drawWidth = image.width * scale
            val drawHeight = image.height * scale
            val left = (size.width - drawWidth) / 2 + pan.x
            val top = (size.height - drawHeight) / 2 + pan.y

            drawImage(
                image = image,
                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = IntSize(drawWidth.roundToInt().coerceAtLeast(1), drawHeight.roundToInt().coerceAtLeast(1)),
                filterQuality = FilterQuality.High,
            )
        }

        if (image == null && placeholder.isNotEmpty()) {
            Dim(
                placeholder,
                modifier = Modifier.padding(32.dp),
                size = 14,
                color = Palette.TextDim,
                align = TextAlign.Center,
            )
        }
    }
}

private fun DrawScope.drawBackdrop(mode: BackdropMode) {
    when (mode) {
        BackdropMode.WHITE -> drawRect(Color.White)
        BackdropMode.BLACK -> drawRect(Color.Black)
        BackdropMode.DARK -> drawRect(Palette.Panel2)
        BackdropMode.CHECKER -> {
            drawRect(CHECKER_A)
            var y = 0f
            var row = 0
            while (y < size.height) {
                var x = if (row % 2 == 0) CHECKER_SIZE else 0f
                while (x < size.width) {
                    drawRect(
                        color = CHECKER_B,
                        topLeft = Offset(x, y),
                        size = Size(
                            minOf(CHECKER_SIZE, size.width - x),
                            minOf(CHECKER_SIZE, size.height - y),
                        ),
                    )
                    x += CHECKER_SIZE * 2
                }
                y += CHECKER_SIZE
                row++
            }
        }
    }
}

/** The little doughnut next to the "Saved" percentage. */
@Composable
public fun SavedPie(percent: Double?, modifier: Modifier = Modifier) {
    Canvas(modifier.background(Color.Transparent)) {
        val diameter = minOf(size.width, size.height)
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val arcSize = Size(diameter, diameter)
        drawArc(
            color = Palette.Border,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = true,
            topLeft = topLeft,
            size = arcSize,
        )
        if (percent == null) return@Canvas
        val sweep = (percent.coerceIn(0.0, 100.0) / 100.0 * 360).toFloat()
        drawArc(
            color = if (percent > 0) Palette.Green else Palette.Red,
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = true,
            topLeft = topLeft,
            size = arcSize,
        )
    }
}
