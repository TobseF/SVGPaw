package io.github.tobsef.svgokt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.hoverable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import io.github.tobsef.svgokt.ui.theme.Palette

/** The red, all-caps step heading — "1 · FILES". */
@Composable
public fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = Palette.Red,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = modifier,
    )
}

/**
 * A hover tooltip.
 *
 * Built on [Popup] and pointer hover rather than Material's tooltip so it stays on stable APIs and
 * can carry the multi-line help text the plugin catalogue supplies.
 */
@Composable
public fun Tooltip(
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (text.isEmpty()) {
        Box(modifier) { content() }
        return
    }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(modifier.hoverable(interaction)) {
        content()
        if (hovered) {
            Popup(popupPositionProvider = BelowCursorPosition) {
                Box(
                    Modifier
                        .widthIn(max = 320.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Palette.Panel2)
                        .border(1.dp, Palette.Border, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                ) {
                    Text(text, color = Palette.Text, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}

/** Places the tooltip under the anchor, nudged back on screen when it would overflow. */
private object BelowCursorPosition : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: androidx.compose.ui.unit.IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: androidx.compose.ui.unit.IntSize,
    ): IntOffset {
        val x = anchorBounds.left.coerceAtMost(windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val below = anchorBounds.bottom + 6
        val y = if (below + popupContentSize.height <= windowSize.height) {
            below
        } else {
            (anchorBounds.top - popupContentSize.height - 6).coerceAtLeast(0)
        }
        return IntOffset(x, y)
    }
}

/**
 * The app's button.
 *
 * Material's own buttons carry a ripple and elevation that fight the flat, dense look the Python
 * front-end had, so this is a plain clickable surface with a hover tint.
 */
@Composable
public fun PawButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    enabled: Boolean = true,
    height: Int = 34,
    fontSize: Int = 13,
    bold: Boolean = accent,
    tooltip: String = "",
    contentColor: Color? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val background = when {
        !enabled -> Palette.Panel2
        accent && hovered -> Palette.RedHover
        accent -> Palette.Red
        hovered -> Palette.Border
        else -> Palette.Panel2
    }
    val foreground = when {
        !enabled -> Palette.TextDim
        contentColor != null -> contentColor
        accent -> Palette.White
        else -> Palette.Text
    }

    Tooltip(tooltip) {
        Box(
            modifier = modifier
                .height(height.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(background)
                .hoverable(interaction, enabled = enabled)
                .clickable(enabled = enabled) { onClick() }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                color = foreground,
                fontSize = fontSize.sp,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** A borderless button that only reveals itself on hover — used for "Collapse all" and the gear. */
@Composable
public fun GhostButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tooltip: String = "",
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Tooltip(tooltip) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (hovered) Palette.Panel2 else Color.Transparent)
                .hoverable(interaction)
                .clickable { onClick() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

/** The pill-shaped on/off switch from the features list. */
@Composable
public fun PawSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(width = 36.dp, height = 20.dp)
            .clip(CircleShape)
            .background(if (checked) Palette.Red else Palette.Border)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(horizontal = 2.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(if (checked) Palette.White else Color(0xFFBDBDBD)),
        )
    }
}

/** Switch plus label, the whole row clickable. */
@Composable
public fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    tooltip: String = "",
    labelColor: Color = Palette.Text,
) {
    Tooltip(tooltip, modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PawSwitch(checked, onCheckedChange)
            Spacer(Modifier.width(10.dp))
            Text(label, color = labelColor, fontSize = 13.sp)
        }
    }
}

/** A square checkbox, used in the output-settings dialog. */
@Composable
public fun PawCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (checked) Palette.Red else Color.Transparent)
                .border(
                    width = if (checked) 0.dp else 2.dp,
                    color = if (checked) Color.Transparent else Palette.Border,
                    shape = RoundedCornerShape(5.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Text("✓", color = Palette.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Text(label, color = Palette.Text, fontSize = 14.sp)
    }
}

/** The Original/Optimized and Image/Markup toggles. */
@Composable
public fun SegmentedToggle(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    tooltip: String = "",
) {
    Tooltip(tooltip) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Palette.Panel)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            for (option in options) {
                val isSelected = option == selected
                val background = when {
                    isSelected && accent -> Palette.Red
                    isSelected -> Palette.Panel2
                    else -> Color.Transparent
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(background)
                        .clickable { onSelect(option) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        option,
                        color = if (isSelected && accent) Palette.White else Palette.Text,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

/** A framed panel — the stage, the stats row, the file list. */
@Composable
public fun Card(
    modifier: Modifier = Modifier,
    color: Color = Palette.Panel,
    corner: Int = 12,
    bordered: Boolean = false,
    padding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(corner.dp)
    Box(
        modifier
            .clip(shape)
            .background(color)
            .then(if (bordered) Modifier.border(1.dp, Palette.Border, shape) else Modifier)
            .padding(padding),
    ) { content() }
}

/** Muted body copy. */
@Composable
public fun Dim(
    text: String,
    modifier: Modifier = Modifier,
    size: Int = 11,
    color: Color = Palette.TextDim,
    align: TextAlign? = null,
) {
    Text(
        text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        lineHeight = (size + 4).sp,
        textAlign = align,
        style = LocalTextStyle.current,
    )
}

/** A thin indeterminate/determinate bar that keeps its height while idle, so nothing jumps. */
@Composable
public fun ProgressStrip(
    active: Boolean,
    progress: Float?,
    phase: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(if (active) Palette.Panel2 else Palette.Bg),
    ) {
        if (!active) return@Box
        if (progress != null) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(Palette.Red),
            )
        } else {
            // Indeterminate: a short bar sliding left to right.
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.fillMaxWidth(phase.coerceIn(0f, 0.75f)))
                    Box(
                        Modifier
                            .fillMaxWidth(0.25f)
                            .height(4.dp)
                            .background(Palette.Red),
                    )
                }
            }
        }
    }
}

/** A labelled value in the stats row. */
@Composable
public fun StatCell(
    heading: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Palette.Text,
    tooltip: String = "",
    trailing: @Composable (() -> Unit)? = null,
) {
    Tooltip(tooltip, modifier) {
        Column {
            Text(
                heading.uppercase(),
                color = Palette.TextDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    value,
                    color = valueColor,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp),
                )
                if (trailing != null) {
                    Spacer(Modifier.width(14.dp))
                    trailing()
                }
            }
        }
    }
}
