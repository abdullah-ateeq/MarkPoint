package com.example.markpoint.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.TableRows
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.markpoint.data.AppSettings
import com.example.markpoint.domain.MarkPointMode
import com.example.markpoint.domain.MarkPointResult
import com.example.markpoint.domain.MarkPosition
import com.example.markpoint.domain.MeasurementFormatter
import com.example.markpoint.domain.MeasurementUnit
import com.example.markpoint.domain.ResultTextBuilder
import com.example.markpoint.domain.SpacingStrategy
import com.example.markpoint.ui.CalculatorState
import com.example.markpoint.ui.MarkPointViewModel
import com.example.markpoint.ui.components.MeasurementLine
import com.example.markpoint.ui.components.NoticeRow
import com.example.markpoint.ui.components.ScreenHeader
import com.example.markpoint.ui.components.SectionTitle
import com.example.markpoint.ui.components.SegmentedSelector
import com.example.markpoint.ui.components.copyToClipboard
import com.example.markpoint.ui.components.rememberHaptic
import com.example.markpoint.ui.theme.FormulaText
import com.example.markpoint.ui.theme.MeasurementValue
import com.example.markpoint.ui.theme.MeasurementValueSmall
import com.example.markpoint.ui.theme.SectionLabel
import kotlinx.coroutines.launch

private val QUICK_MARK_COUNTS = listOf(2, 3, 4, 5, 10)
private const val COLLAPSED_STEP_COUNT = 10

@Composable
fun CalculatorScreen(
    viewModel: MarkPointViewModel,
    state: CalculatorState,
    settings: AppSettings,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic(settings.haptics)

    fun showMessage(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    val actions = ResultActions(
        onCopy = {
            viewModel.summaryText()?.let {
                copyToClipboard(context, "MarkPoint marks", it)
                showMessage("Marks copied")
            }
        },
        onCopyValues = {
            viewModel.valuesText()?.let {
                copyToClipboard(context, "MarkPoint values", it)
                showMessage("Values copied")
            }
        },
        onSave = {
            if (viewModel.save()) {
                haptic()
                showMessage("Calculation saved")
            }
        }
    )

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        if (wide) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ScreenHeader("MarkPoint", "Create evenly spaced measurement marks.")
                    InputSection(
                        viewModel = viewModel,
                        state = state,
                        onCalculate = {
                            focusManager.clearFocus()
                            haptic()
                            viewModel.calculate()
                        },
                        onReset = {
                            focusManager.clearFocus()
                            haptic()
                            viewModel.reset()
                        },
                        onModeChange = {
                            haptic()
                            viewModel.setMode(it)
                        }
                    )
                }
                Column(
                    Modifier.weight(1.15f).fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val result = state.result
                    if (result != null) {
                        ResultSection(result, state.unit, settings, actions)
                    } else {
                        EmptyResult()
                    }
                }
            }
        } else {
            val scrollState = rememberScrollState()
            var resultOffset by remember { mutableIntStateOf(0) }
            Column(
                Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ScreenHeader("MarkPoint", "Create evenly spaced measurement marks.")
                InputSection(
                    viewModel = viewModel,
                    state = state,
                    onCalculate = {
                        focusManager.clearFocus()
                        haptic()
                        if (viewModel.calculate()) scope.launch { scrollToResult(scrollState) { resultOffset } }
                    },
                    onReset = {
                        focusManager.clearFocus()
                        haptic()
                        viewModel.reset()
                        scope.launch { scrollState.animateScrollTo(0) }
                    },
                    onModeChange = {
                        haptic()
                        viewModel.setMode(it)
                    }
                )
                val result = state.result
                if (result != null) {
                    Column(
                        Modifier.onGloballyPositioned { resultOffset = it.positionInParent().y.toInt() },
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ResultSection(result, state.unit, settings, actions)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private suspend fun scrollToResult(scrollState: ScrollState, offset: () -> Int) {
    // Wait for the freshly shown result to be laid out so it has a position.
    kotlinx.coroutines.delay(80)
    val target = offset()
    if (target > 0) scrollState.animateScrollTo(target)
}

private class ResultActions(
    val onCopy: () -> Unit,
    val onCopyValues: () -> Unit,
    val onSave: () -> Unit
)

// region Inputs

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InputSection(
    viewModel: MarkPointViewModel,
    state: CalculatorState,
    onCalculate: () -> Unit,
    onReset: () -> Unit,
    onModeChange: (MarkPointMode) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val unit = state.unit
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SegmentedSelector(
            options = MarkPointMode.entries,
            selected = state.mode,
            label = { if (it == MarkPointMode.MARKS) "Marks" else "Spacing" },
            onSelect = onModeChange
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MeasurementField(
                label = "Start",
                value = state.startText,
                onValueChange = viewModel::setStart,
                unit = unit,
                error = state.errors.start,
                accessibilityLabel = "Start measurement",
                onToggleSign = viewModel::toggleStartSign,
                imeAction = ImeAction.Next,
                onDone = {},
                modifier = Modifier.weight(1f)
            )
            MeasurementField(
                label = "End",
                value = state.endText,
                onValueChange = viewModel::setEnd,
                unit = unit,
                error = state.errors.end,
                accessibilityLabel = "End measurement",
                onToggleSign = viewModel::toggleEndSign,
                imeAction = ImeAction.Next,
                onDone = {},
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            if (state.mode == MarkPointMode.MARKS) {
                OutlinedTextField(
                    value = state.marksText,
                    onValueChange = viewModel::setMarks,
                    label = { Text("Number of marks") },
                    singleLine = true,
                    isError = state.errors.marks != null,
                    supportingText = state.errors.marks?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onCalculate() }),
                    textStyle = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { contentDescription = "Number of marks" }
                )
            } else {
                OutlinedTextField(
                    value = state.spacingText,
                    onValueChange = viewModel::setSpacing,
                    label = { Text("Spacing") },
                    singleLine = true,
                    isError = state.errors.spacing != null,
                    supportingText = state.errors.spacing?.let { { Text(it) } },
                    suffix = { Text(unit.symbol) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onCalculate() }),
                    textStyle = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { contentDescription = "Spacing" }
                )
            }
            UnitSelector(unit = unit, onSelect = viewModel::setUnit, modifier = Modifier.width(112.dp))
        }

        if (state.mode == MarkPointMode.MARKS) {
            Text(
                "Number of marks = marked positions after the start, including the end point. " +
                    "The start is the reference and is not counted.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QUICK_MARK_COUNTS.forEach { count ->
                    FilterChip(
                        selected = state.marksText == count.toString(),
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.setMarks(count.toString())
                        },
                        label = { Text(count.toString()) },
                        modifier = Modifier.semantics { contentDescription = "$count marks" }
                    )
                }
            }
        } else {
            SegmentedSelector(
                options = SpacingStrategy.entries,
                selected = state.strategy,
                label = { if (it == SpacingStrategy.EXACT_ENDPOINT) "Exact endpoint" else "Fixed spacing" },
                onSelect = viewModel::setStrategy
            )
            Text(
                if (state.strategy == SpacingStrategy.EXACT_ENDPOINT) {
                    "Marks are distributed evenly so the last mark lands exactly on the end."
                } else {
                    "Keeps your spacing exactly and reports any remaining distance."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state.strategy == SpacingStrategy.FIXED_SPACING) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Switch) { viewModel.setAddEndpoint(!state.addEndpoint) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Add endpoint", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "List the end separately when the spacing does not land on it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = state.addEndpoint, onCheckedChange = null)
                }
            }
        }

        state.errors.general?.let { NoticeRow(Icons.Outlined.ErrorOutline, it, emphasized = true) }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onCalculate,
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                Text("Calculate", style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.height(52.dp).semantics { contentDescription = "Reset calculator" }
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Reset")
            }
        }
    }
}

@Composable
private fun MeasurementField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: MeasurementUnit,
    error: String?,
    accessibilityLabel: String,
    onToggleSign: () -> Unit,
    imeAction: ImeAction,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        suffix = { Text(unit.symbol) },
        trailingIcon = {
            IconButton(
                onClick = onToggleSign,
                modifier = Modifier.semantics { contentDescription = "Toggle negative $label value" }
            ) {
                Text("±", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        textStyle = MaterialTheme.typography.titleMedium,
        modifier = modifier.semantics { contentDescription = accessibilityLabel }
    )
}

@Composable
private fun UnitSelector(unit: MeasurementUnit, onSelect: (MeasurementUnit) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = unit.symbol,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Unit") },
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) },
            textStyle = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )
        // Transparent overlay so a tap anywhere on the field opens the menu.
        Box(
            Modifier
                .matchParentSize()
                .padding(top = 8.dp)
                .clickable(role = Role.DropdownList) { expanded = true }
                .clearAndSetSemantics {
                    contentDescription = "Unit, ${unit.spokenName}"
                    role = Role.DropdownList
                }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MeasurementUnit.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "${option.symbol}  ·  ${option.spokenName}",
                            fontWeight = if (option == unit) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

// endregion

// region Results

@Composable
private fun EmptyResult() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Straighten, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Mark positions appear here", style = MaterialTheme.typography.titleMedium)
            Text(
                "Enter a start, an end and the number of marks or a spacing, then tap Calculate.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResultSection(result: MarkPointResult, unit: MeasurementUnit, settings: AppSettings, actions: ResultActions) {
    val decimals = settings.decimalPlaces
    fun fmt(mm: java.math.BigDecimal) = MeasurementFormatter.formatMeasurement(mm, unit, decimals)

    val heading = when {
        result.mode == MarkPointMode.MARKS -> "Equal spacing"
        result.strategy == SpacingStrategy.FIXED_SPACING -> "Fixed spacing"
        else -> "Spacing · exact endpoint"
    }

    ResultCard {
        SectionTitle(heading)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            StatItem("Interval", result.intervalMm?.let { fmt(it.abs()) } ?: "—", Modifier.weight(1f))
            StatItem("Total distance", fmt(result.totalDistanceMm), Modifier.weight(1f))
            StatItem("Marks", result.spacedMarks.size.toString(), Modifier.weight(0.6f))
        }
        val notices = resultNotices(result, unit, decimals)
        if (notices.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                notices.forEach { (text, emphasized) -> NoticeRow(Icons.Outlined.Info, text, emphasized = emphasized) }
            }
        }
        Spacer(Modifier.height(12.dp))
        ResultActionRow(actions)
    }

    if (settings.showVisual) {
        ResultCard {
            SectionTitle("Measurement line")
            Spacer(Modifier.height(8.dp))
            MeasurementLine(result, unit, decimals, settings.orientation)
            Text(
                "${result.spacedMarks.size} marked position${if (result.spacedMarks.size == 1) "" else "s"}" +
                    if (result.separateEndpoint != null) " + endpoint" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    ResultCard(contentPadding = 0.dp) {
        SectionTitle("Mark positions", Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
        result.marks.forEachIndexed { i, mark ->
            if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            MarkRow(mark, result, unit, decimals)
        }
        Spacer(Modifier.height(8.dp))
    }

    if (settings.showFormula || settings.showSteps) {
        FormulaCard(result, unit, decimals, settings.showFormula, settings.showSteps)
    }

    Text(
        "Use calculated positions as a measuring guide. Verify physical measurements before cutting, drilling, or permanent installation.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun resultNotices(result: MarkPointResult, unit: MeasurementUnit, decimals: Int): List<Pair<String, Boolean>> {
    if (result.mode != MarkPointMode.SPACING) return emptyList()
    val requested = result.requestedSpacingMm ?: return emptyList()
    val requestedText = MeasurementFormatter.formatCompactMeasurement(requested, unit, MeasurementFormatter.MAX_DECIMALS)
    val notices = mutableListOf<Pair<String, Boolean>>()
    when (result.strategy) {
        SpacingStrategy.FIXED_SPACING -> {
            val remaining = result.remainingDistanceMm
            if (remaining != null && remaining.signum() != 0) {
                notices += "$requestedText spacing does not land exactly on the endpoint." to true
                notices += "Remaining distance: ${MeasurementFormatter.formatMeasurement(remaining, unit, decimals)}" to false
            } else {
                notices += "$requestedText spacing lands exactly on the endpoint." to false
            }
        }
        SpacingStrategy.EXACT_ENDPOINT -> {
            if (result.spacingAdjusted) {
                val interval = MeasurementFormatter.formatMeasurement(result.intervalMm!!.abs(), unit, decimals)
                notices += "$requestedText spacing does not land exactly on the endpoint." to true
                notices += "Adjusted to $interval so the last mark lands on the endpoint." to false
            } else {
                notices += "$requestedText spacing lands exactly on the endpoint." to false
            }
        }
        null -> Unit
    }
    return notices
}

@Composable
private fun ResultCard(contentPadding: androidx.compose.ui.unit.Dp = 16.dp, content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(contentPadding)) { content() }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(label.uppercase(), style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MeasurementValueSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultActionRow(actions: ResultActions) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FilledTonalButton(onClick = actions.onCopy, modifier = Modifier.semantics { contentDescription = "Copy marks" }) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Copy")
        }
        OutlinedButton(onClick = actions.onCopyValues, modifier = Modifier.semantics { contentDescription = "Copy values only" }) {
            Icon(Icons.Outlined.TableRows, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Values")
        }
        OutlinedButton(onClick = actions.onSave, modifier = Modifier.semantics { contentDescription = "Save calculation" }) {
            Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Save")
        }
    }
}

@Composable
private fun MarkRow(mark: MarkPosition, result: MarkPointResult, unit: MeasurementUnit, decimals: Int) {
    val value = MeasurementFormatter.formatMeasurement(mark.positionMm, unit, decimals)
    val spoken = MeasurementFormatter.spokenMeasurement(mark.positionMm, unit, decimals)
    val atEnd = !mark.isEndpoint && mark.positionMm.compareTo(result.endMm) == 0
    val description = when {
        mark.isEndpoint -> "Endpoint at $spoken, not equally spaced"
        atEnd -> "Mark ${mark.index} at $spoken, end point"
        else -> "Mark ${mark.index} at $spoken"
    }
    val accent = if (mark.isEndpoint) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 3.dp, height = 28.dp)
                .background(accent, MaterialTheme.shapes.extraSmall)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                ResultTextBuilder.markLabel(mark).uppercase(),
                style = SectionLabel,
                color = if (mark.isEndpoint) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            when {
                mark.isEndpoint -> Text("Not equally spaced", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                atEnd -> Text("End point", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(value, style = MeasurementValue, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun FormulaCard(result: MarkPointResult, unit: MeasurementUnit, decimals: Int, showFormula: Boolean, showSteps: Boolean) {
    val breakdown = remember(result, unit, decimals) { ResultTextBuilder.breakdown(result, unit, decimals) }
    var expanded by rememberSaveable(result) { mutableStateOf(false) }
    ResultCard {
        if (showFormula) {
            SectionTitle("Formula")
            Spacer(Modifier.height(8.dp))
            breakdown.formula.forEach { Text(it, style = FormulaText, color = MaterialTheme.colorScheme.onSurface) }
        }
        if (showSteps && breakdown.steps.isNotEmpty()) {
            if (showFormula) Spacer(Modifier.height(16.dp))
            SectionTitle("Step by step")
            Spacer(Modifier.height(8.dp))
            val visible = if (expanded) breakdown.steps else breakdown.steps.take(COLLAPSED_STEP_COUNT)
            visible.forEachIndexed { i, step ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${i + 1}".padStart(3),
                        style = FormulaText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(horizontal = 6.dp).size(12.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(step, style = FormulaText, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            if (breakdown.steps.size > COLLAPSED_STEP_COUNT) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Show fewer steps" else "Show all ${breakdown.steps.size} steps")
                }
            }
        }
    }
}

// endregion
