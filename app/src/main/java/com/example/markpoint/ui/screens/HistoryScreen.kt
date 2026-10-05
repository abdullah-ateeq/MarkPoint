package com.example.markpoint.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.markpoint.data.HistoryRecord
import com.example.markpoint.data.HistoryRepository
import com.example.markpoint.domain.MarkPointMode
import com.example.markpoint.domain.MeasurementFormatter
import com.example.markpoint.domain.NumberParser
import com.example.markpoint.domain.SpacingStrategy
import com.example.markpoint.ui.components.ScreenHeader
import com.example.markpoint.ui.theme.MeasurementValueSmall
import com.example.markpoint.ui.theme.SectionLabel
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    history: List<HistoryRecord>,
    decimals: Int,
    onOpen: (HistoryRecord) -> Unit,
    onDelete: (HistoryRecord) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ScreenHeader(
                    "History",
                    "${history.size} of ${HistoryRepository.MAX_RECORDS} saved calculations",
                    Modifier.weight(1f)
                )
                if (history.isNotEmpty()) {
                    TextButton(onClick = { confirmClear = true }) { Text("Clear all") }
                }
            }
        }
        if (history.isEmpty()) {
            item { EmptyHistory() }
        }
        items(history, key = { it.id }) { record ->
            HistoryItem(
                record = record,
                decimals = decimals,
                onOpen = { onOpen(record) },
                onDelete = { onDelete(record) },
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().animateItem()
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all history?") },
            text = { Text("All saved calculations will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearAll()
                }) { Text("Clear all") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
        Text("No saved calculations", style = MaterialTheme.typography.titleMedium)
        Text(
            "Tap Save on a result to keep it here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val dateFormat: DateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

/** Text lines describing a record, e.g. "0 → 100 cm", "4 marks", "25 cm interval". */
private fun describe(record: HistoryRecord, decimals: Int): Pair<String, List<String>> {
    val unit = record.unit
    fun mm(text: String?) = text?.let(NumberParser::parseDecimal)
    fun compact(mmText: String?) = mm(mmText)?.let { MeasurementFormatter.formatCompactMeasurement(it, unit, decimals) }
    val marks = record.spacedMarkCount
    val marksText = "$marks mark${if (marks == 1) "" else "s"}"
    val title = when {
        record.mode == MarkPointMode.MARKS -> "Equal marks"
        record.strategy == SpacingStrategy.FIXED_SPACING -> "Fixed spacing"
        else -> "Spacing · exact endpoint"
    }
    val details = buildList {
        add(marksText)
        if (record.mode == MarkPointMode.SPACING) add("${record.spacing} ${unit.symbol} spacing")
        compact(record.intervalMm?.removePrefix("-"))?.let {
            if (record.mode == MarkPointMode.MARKS || record.strategy == SpacingStrategy.EXACT_ENDPOINT) add("$it interval")
        }
        if (record.strategy == SpacingStrategy.FIXED_SPACING && record.mode == MarkPointMode.SPACING) {
            val remaining = mm(record.remainingMm)
            if (remaining != null && remaining.signum() != 0) add("${compact(record.remainingMm)} remaining")
        }
    }
    return title to details
}

@Composable
private fun HistoryItem(
    record: HistoryRecord,
    decimals: Int,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, details) = describe(record, decimals)
    val range = "${record.start} → ${record.end} ${record.unit.symbol}"
    val date = dateFormat.format(Date(record.timestamp))
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = modifier
    ) {
        Row(
            Modifier
                .clickable(onClickLabel = "Open calculation", onClick = onOpen)
                .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier.weight(1f).semantics(mergeDescendants = true) {
                    contentDescription = "$title, $range, ${details.joinToString(", ")}, saved $date"
                }
            ) {
                Text(title.uppercase(), style = SectionLabel, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(range, style = MeasurementValueSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(
                    details.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete calculation")
            }
        }
    }
}
