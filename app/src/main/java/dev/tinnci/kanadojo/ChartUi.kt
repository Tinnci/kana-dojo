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
fun KanaChartScreen(
    script: Script,
    mastery: Map<String, Int>,
    onSpeak: (String) -> Unit,
    onEarcon: (KanaEarcon) -> Unit
) {
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
                onEarcon = onEarcon,
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
            val selected = tappedItem?.id == item.id
            Card(
                onClick = {
                    onEarcon(KanaEarcon.Select)
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
                    Text(localizedChartRowLabel(item.row), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    cardTag?.let { tag ->
                        Text(localizedChartCardTag(tag.label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    }
                    if (item.confusable.isNotEmpty()) {
                        Text(stringResource(R.string.chart_tag_contrast), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Spacer(Modifier.height(8.dp))
                    MasteryPips(level = level)
                    Text(localizedChartMasteryLabel(level), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(localizedChartTapTitle(feedback.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(localizedChartTapMessage(feedback.message), style = MaterialTheme.typography.bodySmall)
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
                Text(localizedChartContrastTitle(copy.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(localizedChartContrastMessage(copy.message), style = MaterialTheme.typography.bodySmall)
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
                Text(localizedChartRowGuidanceTitle(copy.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(localizedChartRowGuidanceMessage(copy.message), style = MaterialTheme.typography.bodySmall)
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
            Text(localizedChartProgress(progressCopy.message), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ChartRowFilters(
    rows: List<String>,
    selectedRow: String?,
    onEarcon: (KanaEarcon) -> Unit,
    onRowChange: (String?) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        item {
            AssistChip(
                onClick = {
                    onEarcon(KanaEarcon.Select)
                    onRowChange(null)
                },
                label = { Text(stringResource(R.string.chart_filter_all)) },
                leadingIcon = {
                    if (selectedRow == null) Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )
        }
        items(rows) { row ->
            AssistChip(
                onClick = {
                    onEarcon(KanaEarcon.Select)
                    onRowChange(row)
                },
                label = { Text(localizedChartRowLabel(row)) },
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
            Text(localizedChartLegendMessage(chartLegendCopyFor().message), style = MaterialTheme.typography.bodySmall)
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

@Composable
private fun localizedChartProgress(message: String): String {
    if (!message.endsWith(" fluent")) return message
    val labelAndCount = message.removeSuffix(" fluent")
    val count = labelAndCount.substringAfterLast(" ", missingDelimiterValue = "")
    val label = labelAndCount.substringBeforeLast(" ", missingDelimiterValue = labelAndCount)
    if (count.isBlank() || "/" !in count) return message
    return stringResource(R.string.chart_progress, localizedChartLabel(label), count)
}

@Composable
private fun localizedChartRowGuidanceTitle(title: String): String {
    val prefix = "No fluent "
    val suffix = " yet"
    if (!title.startsWith(prefix) || !title.endsWith(suffix)) return title
    val rowLabel = title.removePrefix(prefix).removeSuffix(suffix)
    return stringResource(R.string.chart_row_guidance_title, localizedChartLabel(rowLabel))
}

@Composable
private fun localizedChartRowGuidanceMessage(message: String): String =
    when (message) {
        "Use this row as reference, then return to lessons when you want it to count as recall." -> stringResource(R.string.chart_row_guidance_message)
        else -> message
    }

@Composable
private fun localizedChartContrastTitle(title: String): String {
    val count = title.substringBefore(" contrast kana").toIntOrNull() ?: return title
    return stringResource(R.string.chart_contrast_title, count)
}

@Composable
private fun localizedChartContrastMessage(message: String): String {
    val prefix = "Outlined "
    val suffix = " tiles have known lookalikes; compare shape and stroke direction."
    if (!message.startsWith(prefix) || !message.endsWith(suffix)) return message
    val label = message.removePrefix(prefix).removeSuffix(suffix)
    return stringResource(R.string.chart_contrast_message, localizedChartLabel(label))
}

@Composable
private fun localizedChartTapTitle(title: String): String {
    val kana = title.removePrefix("Audio cue: ")
    if (kana == title) return title
    return stringResource(R.string.chart_tap_title, kana)
}

@Composable
private fun localizedChartTapMessage(message: String): String {
    val parts = message.split(" · ")
    if (parts.size != 2) return message
    return stringResource(R.string.chart_tap_message, parts[0], localizedChartLabel(parts[1]))
}

@Composable
private fun localizedChartRowLabel(row: String): String =
    when (row) {
        "vowels" -> stringResource(R.string.chart_row_vowels)
        "w" -> stringResource(R.string.chart_row_wn)
        "special" -> stringResource(R.string.chart_row_special)
        else -> if (row.endsWith("-y")) {
            stringResource(R.string.chart_row_blends, row.substringBefore("-").uppercase())
        } else {
            stringResource(R.string.chart_row_generic, row.uppercase())
        }
    }

@Composable
private fun localizedChartLabel(label: String): String =
    when (label) {
        "All" -> stringResource(R.string.chart_row_all)
        "chart" -> stringResource(R.string.chart_row_chart)
        "Vowels" -> stringResource(R.string.chart_row_vowels)
        "vowels" -> stringResource(R.string.chart_row_vowels_lower)
        "W/N row" -> stringResource(R.string.chart_row_wn)
        "w/n row" -> stringResource(R.string.chart_row_wn_lower)
        "Special marks" -> stringResource(R.string.chart_row_special)
        "special marks" -> stringResource(R.string.chart_row_special_lower)
        else -> when {
            label.endsWith(" blends") -> stringResource(R.string.chart_row_blends, label.substringBefore(" ").uppercase())
            label.endsWith(" row") -> stringResource(R.string.chart_row_generic, label.substringBefore(" ").uppercase())
            else -> label
        }
    }

@Composable
private fun localizedChartCardTag(label: String): String =
    when (label) {
        "small" -> stringResource(R.string.chart_tag_small)
        "long mark" -> stringResource(R.string.chart_tag_long_mark)
        "special" -> stringResource(R.string.chart_tag_special)
        else -> label
    }

@Composable
private fun localizedChartMasteryLabel(level: Int): String {
    val clamped = level.coerceIn(0, 5)
    val resId = when (clamped) {
        0 -> R.string.chart_mastery_new
        1 -> R.string.chart_mastery_familiar
        2 -> R.string.chart_mastery_recall
        3 -> R.string.chart_mastery_contrast
        4 -> R.string.chart_mastery_fluent
        else -> R.string.chart_mastery_mastered
    }
    return stringResource(resId, clamped)
}

@Composable
private fun localizedChartLegendMessage(message: String): String =
    when (message) {
        "Fluent is stable recall; mastered is long-spaced maintenance." -> stringResource(R.string.chart_legend_message)
        else -> message
    }
