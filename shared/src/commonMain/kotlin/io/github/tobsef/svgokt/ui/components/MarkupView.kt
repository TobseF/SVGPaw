package io.github.tobsef.svgokt.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import io.github.tobsef.svgokt.ui.theme.Palette

/** Above this many characters the markup is shown without highlighting. */
private const val HIGHLIGHT_LIMIT = 200_000

/** Above this many characters the markup is truncated. */
private const val MARKUP_LIMIT = 800_000

/** Roughly how much text goes into one lazily rendered block. */
private const val CHUNK_TARGET = 2_000

private val TagColor = Color(0xFFE06C75)
private val AttrColor = Color(0xFFD19A66)
private val ValueColor = Color(0xFF98C379)
private val TextColor = Color(0xFFABB2BF)

/**
 * The Markup tab.
 *
 * Optimized SVG is one enormous line, so the text is cut into chunks at tag boundaries and fed to
 * a [LazyColumn]: only what is on screen is laid out, which is what keeps a megabyte of path data
 * from freezing the window. Chunks are highlighted independently, which is safe because every cut
 * lands between elements.
 */
@Composable
public fun MarkupView(content: String, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val truncated = content.length > MARKUP_LIMIT
    val shown = if (truncated) content.take(MARKUP_LIMIT) else content
    val highlight = shown.length <= HIGHLIGHT_LIMIT

    val chunks = remember(shown, highlight) {
        splitAtTagBoundaries(shown).map { chunk ->
            if (highlight) highlightSvg(chunk) else AnnotatedString(chunk)
        }
    }

    LaunchedEffect(content) { listState.scrollToItem(0) }

    Box(modifier.fillMaxSize()) {
        SelectionContainer {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 20.dp),
            ) {
                items(chunks) { chunk ->
                    Text(
                        text = chunk,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = TextColor,
                    )
                }
                if (truncated) {
                    item {
                        Dim(
                            "… truncated — the file is longer than ${MARKUP_LIMIT / 1000}k characters.",
                            modifier = Modifier.padding(top = 10.dp),
                            size = 12,
                            color = Palette.Red,
                        )
                    }
                }
            }
        }
        ListScrollbar(
            listState,
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 6.dp, horizontal = 4.dp),
        )
    }
}

/** Cut [text] into ~[CHUNK_TARGET]-character pieces, always just after a `>`. */
private fun splitAtTagBoundaries(text: String): List<String> {
    if (text.isEmpty()) return emptyList()
    val chunks = mutableListOf<String>()
    var start = 0
    while (start < text.length) {
        val wanted = (start + CHUNK_TARGET).coerceAtMost(text.length)
        val end = if (wanted >= text.length) {
            text.length
        } else {
            // Take the first tag end at or after the target; fall back to a hard cut.
            val boundary = text.indexOf('>', wanted)
            if (boundary >= 0 && boundary - wanted < CHUNK_TARGET) boundary + 1 else wanted
        }
        chunks += text.substring(start, end)
        start = end
    }
    return chunks
}

private val TAG_RE = Regex("""(</?)([A-Za-z_][\w:.-]*)|(/?>)""")
private val ATTR_RE = Regex("""([A-Za-z_][\w:.-]*)\s*=\s*("[^"]*"|'[^']*')""")

/** A light XML highlighter: element names, attribute names and attribute values. */
private fun highlightSvg(text: String): AnnotatedString = buildAnnotatedString {
    append(text)
    for (match in TAG_RE.findAll(text)) {
        addStyle(SpanStyle(color = TagColor), match.range.first, match.range.last + 1)
    }
    for (match in ATTR_RE.findAll(text)) {
        val name = match.groups[1] ?: continue
        val value = match.groups[2] ?: continue
        addStyle(SpanStyle(color = AttrColor), name.range.first, name.range.last + 1)
        addStyle(SpanStyle(color = ValueColor), value.range.first, value.range.last + 1)
    }
}
