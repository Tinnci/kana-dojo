package dev.tinnci.kanadojo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KanaChartScreen(script: Script, mastery: Map<String, Int>, onSpeak: (String) -> Unit) {
    val items = remember(script) { itemsFor(script) }
    val rows = remember(items) { items.map { it.row }.distinct() }
    var selectedRow by remember(script) { mutableStateOf<String?>(null) }
    var tappedItem by remember(script) { mutableStateOf<KanaItem?>(null) }
    val visibleItems = remember(items, selectedRow) {
        selectedRow?.let { row -> items.filter { it.row == row } } ?: items
    }
    val progressCopy = chartProgressCopyFor(selectedRow, items, mastery)
    val rowGuidance = chartRowGuidanceCopyFor(selectedRow, items, mastery)
    val contrastSummary = chartContrastSummaryCopyFor(selectedRow, items)
    val tapFeedback = tappedItem?.let { chartTapFeedbackFor(it) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 86.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ChartHeader(script = script, progressCopy = progressCopy)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            ChartRowFilters(
                rows = rows,
                selectedRow = selectedRow,
                onRowChange = {
                    selectedRow = it
                    tappedItem = null
                }
            )
        }
        tapFeedback?.let { feedback ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                ChartTapFeedbackPanel(feedback)
            }
        }
        rowGuidance?.let { copy ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                ChartRowGuidancePanel(copy)
            }
        }
        contrastSummary?.let { copy ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                ChartContrastSummaryPanel(copy)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            MasteryLegend()
        }
        items(visibleItems) { item ->
            val level = mastery[item.id] ?: 0
            val cardTag = chartCardTagFor(item)
            val masteryCopy = chartMasteryCopyFor(level)
            val selected = tappedItem?.id == item.id
            Card(
                onClick = {
                    tappedItem = item
                    onSpeak(item.kana)
                },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = chartTileColor(level, item.confusable.isNotEmpty(), selected)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = when {
                                selected -> 2.dp
                                item.confusable.isNotEmpty() -> 1.dp
                                else -> 0.dp
                            },
                            color = when {
                                selected -> MaterialTheme.colorScheme.primary
                                item.confusable.isNotEmpty() -> MaterialTheme.colorScheme.tertiary
                                else -> Color.Transparent
                            },
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(item.kana, fontSize = 42.sp, fontWeight = FontWeight.Black)
                    Text(item.romaji, style = MaterialTheme.typography.labelLarge)
                    Text(chartRowLabelFor(item.row), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    cardTag?.let { tag ->
                        Text(tag.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    }
                    if (item.confusable.isNotEmpty()) {
                        Text(stringResource(R.string.chart_tag_contrast), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Spacer(Modifier.height(8.dp))
                    MasteryPips(level = level)
                    Text(masteryCopy.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ChartTapFeedbackPanel(feedback: ChartTapFeedback) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(feedback.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(feedback.message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ChartContrastSummaryPanel(copy: ChartContrastSummaryCopy) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFE7DEFF),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(copy.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(copy.message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ChartRowGuidancePanel(copy: ChartRowGuidanceCopy) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(copy.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(copy.message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ChartHeader(script: Script, progressCopy: ChartProgressCopy) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.chart_header_title, stringResource(script.displayNameResId)),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
            }
            Text(progressCopy.message, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ChartRowFilters(rows: List<String>, selectedRow: String?, onRowChange: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        item {
            AssistChip(
                onClick = { onRowChange(null) },
                label = { Text(stringResource(R.string.chart_filter_all)) },
                leadingIcon = {
                    if (selectedRow == null) Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )
        }
        items(rows) { row ->
            AssistChip(
                onClick = { onRowChange(row) },
                label = { Text(chartRowLabelFor(row)) },
                leadingIcon = {
                    if (selectedRow == row) Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )
        }
    }
}

@Composable
private fun MasteryLegend() {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item { LegendSwatch(stringResource(R.string.chart_legend_new), MaterialTheme.colorScheme.surface) }
                item { LegendSwatch(stringResource(R.string.chart_legend_learning), Color(0xFFFFF1BC)) }
                item { LegendSwatch(stringResource(R.string.chart_legend_fluent), Color(0xFFDCEBDD)) }
                item { LegendSwatch(stringResource(R.string.chart_legend_contrast), Color(0xFFE7DEFF)) }
            }
            Text(chartLegendCopyFor().message, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LegendSwatch(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun chartTileColor(level: Int, confusable: Boolean, selected: Boolean): Color =
    when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        level >= 4 -> Color(0xFFDCEBDD)
        level >= 2 -> Color(0xFFFFF1BC)
        confusable -> Color(0xFFE7DEFF)
        else -> MaterialTheme.colorScheme.surface
    }

@Composable
private fun MasteryPips(level: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        if (index < level) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape
                    )
            )
        }
    }
}
