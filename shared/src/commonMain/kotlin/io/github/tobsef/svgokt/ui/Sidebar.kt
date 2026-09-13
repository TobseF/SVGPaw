package io.github.tobsef.svgokt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.hoverable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.tobsef.svgokt.engine.humanSize
import io.github.tobsef.svgokt.engine.svgoVersion
import io.github.tobsef.svgokt.model.SvgPawModel
import io.github.tobsef.svgokt.ui.components.Card
import io.github.tobsef.svgokt.ui.components.Dim
import io.github.tobsef.svgokt.ui.components.GhostButton
import io.github.tobsef.svgokt.ui.components.ListScrollbar
import io.github.tobsef.svgokt.ui.components.PawButton
import io.github.tobsef.svgokt.ui.components.PrecisionRow
import io.github.tobsef.svgokt.ui.components.SectionHeader
import io.github.tobsef.svgokt.ui.components.SwitchRow
import io.github.tobsef.svgokt.ui.components.Tooltip
import io.github.tobsef.svgokt.ui.theme.Palette

/** Column 1: the file list and the global SVGO options. */
@Composable
public fun Sidebar(model: SvgPawModel, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(Palette.Panel)
            .padding(bottom = 16.dp),
    ) {
        Header(model)

        Spacer(Modifier.height(14.dp))
        SectionHeader("1 · Files", Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            PawButton(
                text = "Open SVG files…",
                onClick = model::chooseFiles,
                accent = true,
                height = 40,
                fontSize = 14,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            PawButton(
                text = "✕",
                onClick = model::clearFiles,
                height = 40,
                modifier = Modifier.width(40.dp),
                tooltip = "Remove all files from the list",
            )
        }

        Spacer(Modifier.height(8.dp))
        Dim("…or drag & drop them onto the preview.", Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))

        FileList(
            model,
            Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .padding(horizontal = 18.dp),
        )

        Spacer(Modifier.height(16.dp))
        SectionHeader("2 · Global settings", Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(6.dp))

        Column(Modifier.padding(horizontal = 20.dp)) {
            SwitchRow(
                label = "Multipass",
                checked = model.settings.multipass,
                onCheckedChange = model::setMultipass,
                tooltip = "Run the whole plugin chain repeatedly until the file stops shrinking. " +
                    "Slower, usually a little smaller.",
            )
            SwitchRow(
                label = "Prettify markup",
                checked = model.settings.pretty,
                onCheckedChange = model::setPretty,
                tooltip = "Indent the output instead of putting it on one line. Readable, but larger.",
            )
            SwitchRow(
                label = "Compare gzipped",
                checked = model.compareGzip,
                onCheckedChange = model::setCompareGzip,
                tooltip = "Show the sizes after gzip compression — that is what a web server " +
                    "actually sends.",
            )

            Spacer(Modifier.height(12.dp))
            PrecisionRow(
                label = "Number precision",
                value = model.settings.floatPrecision,
                onValueChange = model::setFloatPrecision,
                tooltip = "Decimal places kept for coordinates and lengths. Lower is smaller but " +
                    "coarser; 3 is a good default.",
            )
            Spacer(Modifier.height(6.dp))
            PrecisionRow(
                label = "Transform precision",
                value = model.settings.transformPrecision,
                onValueChange = model::setTransformPrecision,
                tooltip = "Decimal places kept inside transform() lists. Rounding these too hard " +
                    "visibly shifts shapes.",
            )
        }
    }
}

@Composable
private fun Header(model: SvgPawModel) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 12.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PawLogo(model.platform, size = 30)
            Spacer(Modifier.width(10.dp))
            Text(
                // "Paw" picks up the colour of the paw next to it.
                buildAnnotatedString {
                    append("SVG")
                    withStyle(SpanStyle(color = Palette.Red)) { append("Paw") }
                },
                color = Palette.Text,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            GearButton(model)
        }
        Spacer(Modifier.height(2.dp))
        Dim(
            "$APP_SLOGAN · svgo-kt ${svgoVersion()}",
            Modifier.padding(horizontal = 20.dp),
        )
    }
}

@Composable
private fun GearButton(model: SvgPawModel) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Tooltip("Settings — output folder, file-name prefix, overwriting") {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (hovered) Palette.Panel2 else Color.Transparent)
                .hoverable(interaction)
                .clickable { model.openSettingsDialog() },
            contentAlignment = Alignment.Center,
        ) {
            GearIcon(model.platform, if (hovered) Palette.Red else Palette.Text, size = 18)
        }
    }
}

@Composable
private fun FileList(model: SvgPawModel, modifier: Modifier = Modifier) {
    Card(modifier, color = Palette.Panel2, corner = 8) {
        if (model.docs.isEmpty()) {
            Dim("No files yet.", Modifier.padding(12.dp), size = 12)
            return@Card
        }
        val listState = rememberLazyListState()
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(vertical = 4.dp, horizontal = 2.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                itemsIndexed(model.docs, key = { _, doc -> doc.path }) { index, doc ->
                    val selected = index == model.current
                    val interaction = remember(doc.path) { MutableInteractionSource() }
                    val hovered by interaction.collectIsHoveredAsState()
                    Tooltip(doc.path) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        selected -> Palette.Red
                                        hovered -> Palette.Border
                                        else -> Color.Transparent
                                    },
                            )
                            .hoverable(interaction)
                            .clickable { model.select(index) }
                            .padding(start = 8.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            doc.name,
                            color = if (selected) Palette.White else Palette.Text,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            humanSize(doc.originalBytes),
                            color = if (selected) Palette.White.copy(alpha = 0.8f) else Palette.TextDim,
                            fontSize = 11.sp,
                        )
                        GhostButton(onClick = { model.removeDoc(index) }) {
                            Text(
                                "✕",
                                color = if (selected) Palette.White else Palette.TextDim,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
        }
            ListScrollbar(
                listState,
                Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 2.dp),
            )
        }
    }
}