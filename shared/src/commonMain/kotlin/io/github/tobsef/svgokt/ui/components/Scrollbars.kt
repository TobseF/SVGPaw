package io.github.tobsef.svgokt.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A vertical scrollbar for a [LazyListState].
 *
 * Desktop draws a real, draggable bar; touch platforms have no such affordance and get nothing,
 * which is why this is `expect` rather than a hand-rolled indicator.
 */
@Composable
public expect fun ListScrollbar(state: LazyListState, modifier: Modifier = Modifier)
