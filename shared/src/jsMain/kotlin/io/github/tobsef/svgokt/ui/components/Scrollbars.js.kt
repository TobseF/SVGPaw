package io.github.tobsef.svgokt.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Kotlin/JS has no desktop-style scrollbar; lists scroll by touch or wheel alone. */
@Composable
public actual fun ListScrollbar(state: LazyListState, modifier: Modifier) {
}
