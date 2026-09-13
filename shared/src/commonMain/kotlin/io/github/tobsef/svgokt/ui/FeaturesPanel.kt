package io.github.tobsef.svgokt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.tobsef.svgokt.engine.PLUGINS_BY_CATEGORY
import io.github.tobsef.svgokt.model.SvgPawModel
import io.github.tobsef.svgokt.model.matches
import io.github.tobsef.svgokt.ui.components.Dim
import io.github.tobsef.svgokt.ui.components.GhostButton
import io.github.tobsef.svgokt.ui.components.ListScrollbar
import io.github.tobsef.svgokt.ui.components.PawButton
import io.github.tobsef.svgokt.ui.components.PawTextField
import io.github.tobsef.svgokt.ui.components.SectionHeader
import io.github.tobsef.svgokt.ui.components.SwitchRow
import io.github.tobsef.svgokt.ui.components.Tooltip
import io.github.tobsef.svgokt.ui.theme.Palette

/** Column 3: every SVGO plugin, grouped, filterable and collapsible. */
@Composable
public fun FeaturesPanel(model: SvgPawModel, modifier: Modifier = Modifier) {
    val needle = model.featureFilter.trim().lowercase()
    val filtering = needle.isNotEmpty()

    Column(modifier.fillMaxSize().background(Palette.Panel)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader("3 · Features")
            Spacer(Modifier.weight(1f))
            PawButton(
                "Reset",
                model::resetSettings,
                height = 26,
                fontSize = 12,
                tooltip = "Back to SVGO's preset-default",
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dim(model.featuresSummary, Modifier.weight(1f))
            GhostButton(onClick = model::toggleAllGroups) {
                Dim(if (model.allCollapsed) "Expand all" else "Collapse all")
            }
        }

        PawTextField(
            value = model.featureFilter,
            onValueChange = model::updateFeatureFilter,
            onFocusChanged = model::onTextInputFocus,
            placeholder = "Filter features…",
            height = 32,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))

        val listState = rememberLazyListState()
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                // Room on the right for the scrollbar, so it never sits on top of a switch.
                modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 22.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for ((category, members) in PLUGINS_BY_CATEGORY) {
                    val visible = members.filter { it.matches(needle) }
                    if (visible.isEmpty()) continue
                    // A filter shows every hit; a group's collapsed state only applies when browsing.
                    val collapsed = !filtering && category.key in model.collapsedGroups
                    val (on, total) = model.settings.countFor(members)

                    item(key = "head-${category.key}") {
                        GroupHeader(
                            label = category.label,
                            blurb = category.blurb,
                            count = "$on/$total",
                            collapsed = collapsed,
                            onClick = { if (!filtering) model.toggleGroup(category.key) },
                        )
                    }
                    if (collapsed) continue
                    items(visible.size, key = { visible[it].id }) { index ->
                        val plugin = visible[index]
                        SwitchRow(
                            label = plugin.label,
                            checked = model.settings.isEnabled(plugin.id),
                            onCheckedChange = { model.setPlugin(plugin.id, it) },
                            tooltip = "${plugin.help}\n\n${plugin.id}",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
            ListScrollbar(
                listState,
                Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 4.dp),
            )
        }
    }
}

@Composable
private fun GroupHeader(
    label: String,
    blurb: String,
    count: String,
    collapsed: Boolean,
    onClick: () -> Unit,
) {
    Tooltip(blurb, Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Palette.Panel2)
                .clickable { onClick() }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(16.dp)) {
                Text(if (collapsed) "▸" else "▾", color = Palette.Red, fontSize = 11.sp)
            }
            Text(label, color = Palette.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Dim(count)
        }
    }
}
