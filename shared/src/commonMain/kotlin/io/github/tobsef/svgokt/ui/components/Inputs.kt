package io.github.tobsef.svgokt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.hoverable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import io.github.tobsef.svgokt.engine.PRECISION_MAX
import io.github.tobsef.svgokt.engine.PRECISION_MIN
import io.github.tobsef.svgokt.ui.theme.Palette
import kotlin.math.roundToInt

/** A single-line text field in the app's flat style. */
@Composable
public fun PawTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    height: Int = 34,
    /** Reported so the window can hold back single-key shortcuts while the caret is here. */
    onFocusChanged: (Boolean) -> Unit = {},
) {
    Box(
        modifier
            .height(height.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (enabled) Palette.Panel2 else Palette.Panel)
            .border(1.dp, Palette.Border, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(
                color = if (enabled) Palette.Text else Palette.TextDim,
                fontSize = 13.sp,
            ),
            cursorBrush = SolidColor(Palette.Red),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { onFocusChanged(it.isFocused) },
        )
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(placeholder, color = Palette.TextDim, fontSize = 13.sp)
        }
    }
}

/** "Number precision  3" plus the slider under it. */
@Composable
public fun PrecisionRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    tooltip: String,
    modifier: Modifier = Modifier,
) {
    Tooltip(tooltip, modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, color = Palette.TextDim, fontSize = 13.sp)
                Text(
                    value.toString(),
                    color = Palette.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.roundToInt()) },
                valueRange = PRECISION_MIN.toFloat()..PRECISION_MAX.toFloat(),
                steps = PRECISION_MAX - PRECISION_MIN - 1,
                colors = SliderDefaults.colors(
                    thumbColor = Palette.Red,
                    activeTrackColor = Palette.Red,
                    inactiveTrackColor = Palette.Border,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().height(28.dp),
            )
        }
    }
}

/** The backdrop picker — a flat dropdown. */
@Composable
public fun PawDropdown(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    tooltip: String = "",
) {
    var expanded by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(modifier) {
        Tooltip(tooltip) {
            Row(
                Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (hovered) Palette.Border else Palette.Panel2)
                    .hoverable(interaction)
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(selected, color = Palette.Text, fontSize = 13.sp)
                Spacer(Modifier.width(12.dp))
                Text("⌄", color = Palette.TextDim, fontSize = 13.sp)
            }
        }
        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, 36),
                onDismissRequest = { expanded = false },
            ) {
                Column(
                    Modifier
                        .width(160.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Palette.Panel2)
                        .border(1.dp, Palette.Border, RoundedCornerShape(6.dp))
                        .padding(vertical = 4.dp),
                ) {
                    for (option in options) {
                        val rowInteraction = remember(option) { MutableInteractionSource() }
                        val rowHovered by rowInteraction.collectIsHoveredAsState()
                        Text(
                            option,
                            color = if (option == selected) Palette.Red else Palette.Text,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (rowHovered) Palette.Border else Color.Transparent)
                                .hoverable(rowInteraction)
                                .clickable {
                                    onSelect(option)
                                    expanded = false
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
    }
}
