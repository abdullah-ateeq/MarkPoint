package com.example.markpoint.ui.screens

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.markpoint.data.AppSettings
import com.example.markpoint.data.ThemeMode
import com.example.markpoint.domain.MarkOrientation
import com.example.markpoint.domain.MarkPointMode
import com.example.markpoint.domain.MarkPointValidator
import com.example.markpoint.domain.MeasurementFormatter
import com.example.markpoint.domain.MeasurementUnit
import com.example.markpoint.domain.NumberParser
import com.example.markpoint.domain.SpacingStrategy
import com.example.markpoint.domain.ValidationMessages
import com.example.markpoint.ui.MarkPointViewModel
import com.example.markpoint.ui.components.ScreenHeader
import com.example.markpoint.ui.components.SectionTitle
import com.example.markpoint.ui.components.SegmentedSelector

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onResetSettings: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScreenHeader("Settings", "Saved on this device.")

            SettingsGroup("Appearance") {
                SettingLabel("Theme")
                SegmentedSelector(
                    options = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM),
                    selected = settings.themeMode,
                    label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                    onSelect = { mode -> onUpdate { it.copy(themeMode = mode) } }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchSetting("Dynamic color", "Use colors from your wallpaper.", settings.dynamicColor) { v ->
                        onUpdate { it.copy(dynamicColor = v) }
                    }
                }
            }

            SettingsGroup("Calculator defaults") {
                SettingLabel("Default mode")
                SegmentedSelector(
                    options = MarkPointMode.entries,
                    selected = settings.defaultMode,
                    label = { if (it == MarkPointMode.MARKS) "Marks" else "Spacing" },
                    onSelect = { mode -> onUpdate { it.copy(defaultMode = mode) } }
                )
                SettingLabel("Default unit")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MeasurementUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = settings.defaultUnit == unit,
                            onClick = { onUpdate { it.copy(defaultUnit = unit) } },
                            label = { Text(unit.symbol) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DefaultValueField(
                        label = "Default start",
                        stored = settings.defaultStart,
                        validate = { MarkPointValidator.parseMeasurement(it, ValidationMessages.START_EMPTY).second },
                        onValid = { v -> onUpdate { it.copy(defaultStart = v) } },
                        modifier = Modifier.weight(1f)
                    )
                    DefaultValueField(
                        label = "Default end",
                        stored = settings.defaultEnd,
                        validate = { MarkPointValidator.parseMeasurement(it, ValidationMessages.END_EMPTY).second },
                        onValid = { v -> onUpdate { it.copy(defaultEnd = v) } },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DefaultValueField(
                        label = "Default marks",
                        stored = settings.defaultMarks.toString(),
                        validate = { MarkPointValidator.validateMarkCount(it).second },
                        onValid = { v -> NumberParser.parseInt(v)?.let { n -> onUpdate { it.copy(defaultMarks = n) } } },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    DefaultValueField(
                        label = "Default spacing",
                        stored = settings.defaultSpacing,
                        validate = { MarkPointValidator.validateSpacing(it).second },
                        onValid = { v -> onUpdate { it.copy(defaultSpacing = v) } },
                        modifier = Modifier.weight(1f)
                    )
                }
                SettingLabel("Spacing method")
                SegmentedSelector(
                    options = SpacingStrategy.entries,
                    selected = settings.spacingStrategy,
                    label = { if (it == SpacingStrategy.EXACT_ENDPOINT) "Exact endpoint" else "Fixed spacing" },
                    onSelect = { s -> onUpdate { it.copy(spacingStrategy = s) } }
                )
            }

            SettingsGroup("Results") {
                SettingLabel("Decimal places")
                SegmentedSelector(
                    options = (MeasurementFormatter.MIN_DECIMALS..MeasurementFormatter.MAX_DECIMALS).toList(),
                    selected = settings.decimalPlaces,
                    label = { it.toString() },
                    onSelect = { d -> onUpdate { it.copy(decimalPlaces = d) } }
                )
                SettingLabel("Visual orientation")
                SegmentedSelector(
                    options = MarkOrientation.entries,
                    selected = settings.orientation,
                    label = { if (it == MarkOrientation.HORIZONTAL) "Horizontal" else "Vertical" },
                    onSelect = { o -> onUpdate { it.copy(orientation = o) } }
                )
                SwitchSetting("Show visual", "Measurement line above the mark list.", settings.showVisual) { v ->
                    onUpdate { it.copy(showVisual = v) }
                }
                SwitchSetting("Show formula", "Interval formula with your values.", settings.showFormula) { v ->
                    onUpdate { it.copy(showFormula = v) }
                }
                SwitchSetting("Show step explanation", "Each mark as start + offset.", settings.showSteps) { v ->
                    onUpdate { it.copy(showSteps = v) }
                }
                SwitchSetting("Haptic feedback", "Light feedback on calculate, save, reset and mode change.", settings.haptics) { v ->
                    onUpdate { it.copy(haptics = v) }
                }
            }

            SettingsGroup("Data") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.weight(1f)) { Text("Reset settings") }
                    OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.weight(1f)) { Text("Clear history") }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Reset settings?",
            text = "All settings return to their defaults. History is kept.",
            confirm = "Reset",
            onConfirm = onResetSettings,
            onDismiss = { confirmReset = false }
        )
    }
    if (confirmClear) {
        ConfirmDialog(
            title = "Clear history?",
            text = "All saved calculations will be permanently removed.",
            confirm = "Clear",
            onConfirm = onClearHistory,
            onDismiss = { confirmClear = false }
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(title)
            content()
        }
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun SwitchSetting(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** Edits a stored default; only valid values are persisted, invalid ones show an error. */
@Composable
private fun DefaultValueField(
    label: String,
    stored: String,
    validate: (String) -> String?,
    onValid: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal
) {
    var text by rememberSaveable(stored) { mutableStateOf(stored) }
    val error = validate(text)
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = if (keyboardType == KeyboardType.Number) input.filter(Char::isDigit).take(4)
            else MarkPointViewModel.sanitizeDecimal(input, allowNegative = true)
            if (validate(text) == null) onValid(NumberParser.normalize(text))
        },
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier
    )
}

@Composable
private fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm()
            }) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
