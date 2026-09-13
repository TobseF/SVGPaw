package io.github.tobsef.svgokt.ui.components

import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.tobsef.svgokt.ui.theme.Palette

private val SvgPawScrollbarStyle = ScrollbarStyle(
    minimalHeight = 24.dp,
    thickness = 8.dp,
    shape = RoundedCornerShape(4.dp),
    hoverDurationMillis = 250,
    unhoverColor = Palette.Border,
    hoverColor = Palette.TextDim,
)

@Composable
public actual fun ListScrollbar(state: LazyListState, modifier: Modifier) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier,
        style = SvgPawScrollbarStyle,
    )
}
