package io.github.tobsef.svgokt.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.tobsef.svgokt.engine.humanSize
import io.github.tobsef.svgokt.engine.oneDecimal
import io.github.tobsef.svgokt.engine.savedPercent
import io.github.tobsef.svgokt.engine.svgDimensions
import io.github.tobsef.svgokt.engine.trimNumber
import io.github.tobsef.svgokt.model.BackdropMode
import io.github.tobsef.svgokt.model.SvgPawModel
import io.github.tobsef.svgokt.model.Tone
import io.github.tobsef.svgokt.ui.components.Card
import io.github.tobsef.svgokt.ui.components.Dim
import io.github.tobsef.svgokt.ui.components.ImageViewer
import io.github.tobsef.svgokt.ui.components.MarkupView
import io.github.tobsef.svgokt.ui.components.PawButton
import io.github.tobsef.svgokt.ui.components.PawDropdown
import io.github.tobsef.svgokt.ui.components.ProgressStrip
import io.github.tobsef.svgokt.ui.components.SavedPie
import io.github.tobsef.svgokt.ui.components.SegmentedToggle
import io.github.tobsef.svgokt.ui.components.StatCell
import io.github.tobsef.svgokt.ui.components.ZoomCommand
import io.github.tobsef.svgokt.ui.theme.Palette

private const val GZIP_TOOLTIP =
    "Compare gzipped decides whether these are the raw file sizes or the sizes after gzip " +
        "compression. The file list always shows raw bytes."

/** Column 2: the preview, the viewer controls, the numbers and the save actions. */
@Composable
public fun PreviewPane(model: SvgPawModel, modifier: Modifier = Modifier) {
    val doc = model.doc
    val optimizedReady = doc?.optimized != null
    val showingOptimized = model.showingOptimized && optimizedReady
    val shownText = if (showingOptimized) doc?.optimized else doc?.original
    val shownImage = if (showingOptimized) doc?.optimizedImage else doc?.originalImage
    var zoomCommand by remember { mutableStateOf<ZoomCommand?>(null) }

    Column(modifier.fillMaxSize().background(Palette.Bg).padding(horizontal = 22.dp)) {

        // ---- toolbar -------------------------------------------------------- #
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SegmentedToggle(
                options = listOf("Original", "Optimized"),
                selected = if (model.showingOptimized) "Optimized" else "Original",
                onSelect = { model.showOptimized(it == "Optimized") },
                accent = true,
                tooltip = "Flip between the original and the optimized document · shortcut: Space",
            )
            Spacer(Modifier.width(12.dp))
            SegmentedToggle(
                options = listOf("Image", "Markup"),
                selected = if (model.markupMode) "Markup" else "Image",
                onSelect = { model.showMarkup(it == "Markup") },
            )
            Spacer(Modifier.weight(1f))
            val shownBytes = if (showingOptimized) doc?.optimizedBytes else doc?.originalBytes
            Dim(infoLine(shownText, shownBytes, showingOptimized), size = 13)
        }

        // ---- stage ----------------------------------------------------------- #
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Card(Modifier.fillMaxSize(), bordered = true) {
                when {
                    doc == null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Dim(
                            "Open an SVG file,\nor drag & drop one here.",
                            size = 14,
                            align = TextAlign.Center,
                        )
                    }

                    model.markupMode -> MarkupView(shownText.orEmpty())

                    else -> ImageViewer(
                        image = shownImage,
                        placeholder = previewPlaceholder(doc.renderStamp != null),
                        backdrop = model.backdrop,
                        fitRequest = model.fitRequest,
                        zoomCommand = zoomCommand,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    )
                }
            }
            if (model.working) {
                Spinner(
                    "Updating preview…",
                    Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
                )
            }
        }

        // ---- viewer controls --------------------------------------------------- #
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PawDropdown(
                options = BackdropMode.entries.map { it.label },
                selected = model.backdrop.label,
                onSelect = { label ->
                    BackdropMode.entries.firstOrNull { it.label == label }?.let(model::setBackdrop)
                },
                tooltip = "Backdrop behind the image",
                modifier = Modifier.width(150.dp),
            )
            Spacer(Modifier.width(8.dp))
            PawButton("−", { zoomCommand = zoomCommand.next(1 / 1.3f) }, Modifier.width(44.dp), tooltip = "Zoom out")
            Spacer(Modifier.width(8.dp))
            PawButton("Fit", model::requestFit, Modifier.width(56.dp), tooltip = "Reset zoom and position · double-click the image")
            Spacer(Modifier.width(8.dp))
            PawButton("+", { zoomCommand = zoomCommand.next(1.3f) }, Modifier.width(44.dp), tooltip = "Zoom in")
            Spacer(Modifier.weight(1f))
            PawButton(
                "Copy markup",
                model::copyMarkup,
                enabled = doc != null,
                tooltip = "Copy the shown SVG source to the clipboard",
            )
        }

        // ---- stats ------------------------------------------------------------- #
        Stats(model, Modifier.fillMaxWidth().padding(top = 10.dp))

        // ---- actions ------------------------------------------------------------ #
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PawButton(
                "Save",
                model::saveCurrent,
                accent = true,
                enabled = optimizedReady && !model.busy,
                height = 42,
                fontSize = 14,
                modifier = Modifier.width(150.dp),
                tooltip = "Write the optimized file using the prefix from Settings · Ctrl+S",
            )
            Spacer(Modifier.width(10.dp))
            PawButton(
                "Save as…",
                model::saveAs,
                enabled = optimizedReady && !model.busy,
                height = 42,
                modifier = Modifier.width(110.dp),
            )
            Spacer(Modifier.width(10.dp))
            PawButton(
                if (model.docs.isEmpty()) "Save all" else "Save all (${model.docs.size})",
                model::saveAll,
                enabled = model.docs.isNotEmpty() && !model.busy,
                height = 42,
                modifier = Modifier.width(140.dp),
                tooltip = "Optimize every loaded file with the current settings and write them " +
                    "all · Ctrl+Shift+S",
            )
            Spacer(Modifier.weight(1f))
            Dim(model.targetHint(), align = TextAlign.End)
        }

        // ---- status -------------------------------------------------------------- #
        Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 16.dp)) {
            val active = model.working || model.busy
            val transition = rememberInfiniteTransition()
            val phase by transition.animateFloat(
                initialValue = 0f,
                targetValue = 0.75f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1100, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            )
            ProgressStrip(active, model.saveProgress, phase)
            Spacer(Modifier.height(6.dp))
            Text(
                model.status.text,
                color = when (model.status.tone) {
                    Tone.Good -> Palette.Green
                    Tone.Bad -> Palette.Red
                    Tone.Normal -> Palette.TextDim
                },
                fontSize = 13.sp,
            )
        }
    }
}

/** The next zoom press, carrying a fresh sequence number so repeats register. */
private fun ZoomCommand?.next(factor: Float): ZoomCommand =
    ZoomCommand(seq = (this?.seq ?: 0) + 1, factor = factor)

@Composable
private fun Stats(model: SvgPawModel, modifier: Modifier = Modifier) {
    val doc = model.doc
    val note = if (model.compareGzip) " · GZIPPED" else ""
    val (before, after) = doc?.let { model.statSizes(it) } ?: (null to null)
    val saved = if (before != null && after != null) savedPercent(before, after) else null

    Card(modifier, padding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCell(
                heading = "Original$note",
                value = before?.let(::humanSize) ?: "–",
                tooltip = GZIP_TOOLTIP,
                modifier = Modifier.weight(1f),
            )
            StatCell(
                heading = "Optimized$note",
                value = after?.let(::humanSize) ?: "–",
                tooltip = GZIP_TOOLTIP,
                modifier = Modifier.weight(1f),
            )
            StatCell(
                heading = "Saved$note",
                value = when {
                    saved == null -> "–"
                    saved >= 0 -> "${oneDecimal(saved)}%"
                    else -> "+${oneDecimal(-saved)}%"
                },
                valueColor = when {
                    saved == null -> Palette.Text
                    saved > 0 -> Palette.Green
                    else -> Palette.Red
                },
                tooltip = GZIP_TOOLTIP,
                modifier = Modifier.weight(1f),
                trailing = { SavedPie(saved, Modifier.size(30.dp)) },
            )
        }
    }
}

/** The floating "Updating preview…" chip over the stage. */
@Composable
private fun Spinner(text: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val dots by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3.99f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
    )
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Palette.Panel2.copy(alpha = 0.94f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text.trimEnd('…') + ".".repeat(dots.toInt().coerceIn(0, 3)),
            color = Palette.Text,
            fontSize = 12.sp,
        )
    }
}

private fun infoLine(text: String?, bytes: Int?, showingOptimized: Boolean): String {
    if (text == null) return ""
    val dims = svgDimensions(text)
        ?.let { (w, h) -> "${trimNumber(w)} × ${trimNumber(h)}" }
        ?: "?"
    val label = if (showingOptimized) "Optimized" else "Original"
    return "$label · $dims · ${humanSize(bytes)}"
}

private fun previewPlaceholder(rendered: Boolean): String = if (rendered) {
    "Preview could not be rendered.\nThe file itself is fine — switch to Markup."
} else {
    "Rendering…"
}
