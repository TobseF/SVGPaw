package io.github.tobsef.svgokt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.tobsef.svgokt.engine.shortPath
import io.github.tobsef.svgokt.model.SvgPawModel
import io.github.tobsef.svgokt.ui.components.Card
import io.github.tobsef.svgokt.ui.components.Dim
import io.github.tobsef.svgokt.ui.components.PawButton
import io.github.tobsef.svgokt.ui.components.PawCheckbox
import io.github.tobsef.svgokt.ui.components.PawTextField
import io.github.tobsef.svgokt.ui.components.SectionHeader
import io.github.tobsef.svgokt.ui.theme.Palette

/**
 * Where optimized files go and how they are named.
 *
 * Edits are held locally and only pushed into the config on Save, so Cancel really cancels.
 */
@Composable
public fun OutputSettingsDialog(model: SvgPawModel, modifier: Modifier = Modifier) {
    val config = model.config
    var useSourceDir by remember { mutableStateOf(config.useSourceDir) }
    var outputDir by remember { mutableStateOf(config.outputDir) }
    var prefix by remember { mutableStateOf(config.prefix) }
    var suffix by remember { mutableStateOf(config.suffix) }
    var overwrite by remember { mutableStateOf(config.overwrite) }

    Box(modifier.widthIn(max = 620.dp).padding(20.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Text("Settings", color = Palette.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Dim("Where optimized files go and how they are named.", size = 13)
            Spacer(Modifier.height(14.dp))

            Card(Modifier.fillMaxWidth(), corner = 10, padding = PaddingValues(16.dp)) {
                Column(Modifier.fillMaxWidth()) {
                    SectionHeader("Output folder")
                    Spacer(Modifier.height(6.dp))
                    PawCheckbox(
                        checked = useSourceDir,
                        onCheckedChange = { useSourceDir = it },
                        label = "Save next to the original file",
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PawTextField(
                            value = outputDir,
                            onValueChange = { outputDir = it },
                            enabled = !useSourceDir,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        PawButton(
                            "Browse",
                            onClick = { model.browseOutputDir(outputDir) { outputDir = it } },
                            enabled = !useSourceDir,
                            modifier = Modifier.width(88.dp),
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    SectionHeader("File name")
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Dim("Prefix", size = 13)
                        Spacer(Modifier.width(10.dp))
                        PawTextField(prefix, { prefix = it }, Modifier.weight(1f))
                        Spacer(Modifier.width(16.dp))
                        Dim("Suffix", size = 13)
                        Spacer(Modifier.width(10.dp))
                        PawTextField(suffix, { suffix = it }, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    NamePreview(useSourceDir, outputDir, prefix, suffix)

                    Spacer(Modifier.height(18.dp))
                    SectionHeader("Overwriting")
                    Spacer(Modifier.height(6.dp))
                    PawCheckbox(
                        checked = overwrite,
                        onCheckedChange = { overwrite = it },
                        label = "Overwrite existing files without asking",
                    )
                    Spacer(Modifier.height(4.dp))
                    Dim(
                        "When this is off, $APP_NAME asks before replacing a file that already exists.",
                        size = 12,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                PawButton(
                    "Save",
                    onClick = {
                        model.applyOutputSettings(useSourceDir, outputDir, prefix, suffix, overwrite)
                    },
                    accent = true,
                    height = 40,
                    modifier = Modifier.width(180.dp),
                )
                Spacer(Modifier.width(12.dp))
                PawButton(
                    "Cancel",
                    onClick = model::closeSettingsDialog,
                    height = 40,
                    modifier = Modifier.width(150.dp),
                )
            }
        }
    }
}

@Composable
private fun NamePreview(useSourceDir: Boolean, outputDir: String, prefix: String, suffix: String) {
    val where = if (useSourceDir) {
        "next to the original"
    } else {
        shortPath(outputDir.trim(), 3).ifEmpty { "(no folder selected)" }
    }
    Column(Modifier.fillMaxWidth().background(Palette.Panel)) {
        Dim("drawing.svg  →  $prefix" + "drawing$suffix.svg", size = 12)
        Dim("    Saved in: $where", size = 12)
        if (prefix.isEmpty() && suffix.isEmpty()) {
            Dim(
                "⚠ No prefix or suffix — this replaces the original file.",
                size = 12,
                color = Palette.Red,
            )
        }
    }
}
