package com.example.markpoint.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.markpoint.MarkPointApplication
import com.example.markpoint.data.AppSettings
import com.example.markpoint.data.HistoryRecord
import com.example.markpoint.data.HistoryRepository
import com.example.markpoint.data.SettingsRepository
import com.example.markpoint.domain.InputErrors
import com.example.markpoint.domain.MarkPointCalculator
import com.example.markpoint.domain.MarkPointMode
import com.example.markpoint.domain.MarkPointResult
import com.example.markpoint.domain.MarkPointValidator
import com.example.markpoint.domain.MeasurementFormatter
import com.example.markpoint.domain.MeasurementUnit
import com.example.markpoint.domain.NumberParser
import com.example.markpoint.domain.ResultTextBuilder
import com.example.markpoint.domain.SpacingStrategy
import com.example.markpoint.domain.UnitConverter
import com.example.markpoint.domain.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalculatorState(
    val initialized: Boolean = false,
    val mode: MarkPointMode = MarkPointMode.MARKS,
    val startText: String = "0",
    val endText: String = "100",
    val marksText: String = "4",
    val spacingText: String = "10",
    val unit: MeasurementUnit = MeasurementUnit.CENTIMETER,
    val strategy: SpacingStrategy = SpacingStrategy.EXACT_ENDPOINT,
    val addEndpoint: Boolean = false,
    val errors: InputErrors = InputErrors(),
    val result: MarkPointResult? = null,
    /** After the first Calculate, edits recalculate immediately so results are never stale. */
    val hasCalculated: Boolean = false
)

class MarkPointViewModel(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    /** Null until settings are loaded from disk, so the first frame never uses the wrong theme. */
    val settings: StateFlow<AppSettings?> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val history: StateFlow<List<HistoryRecord>> =
        historyRepository.history.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val currentSettings: AppSettings get() = settings.value ?: AppSettings()

    init {
        viewModelScope.launch {
            val loaded = settingsRepository.settings.first()
            if (!_state.value.initialized) _state.value = defaultState(loaded)
        }
    }

    private fun defaultState(s: AppSettings) = CalculatorState(
        initialized = true,
        mode = s.defaultMode,
        startText = s.defaultStart,
        endText = s.defaultEnd,
        marksText = s.defaultMarks.toString(),
        spacingText = s.defaultSpacing,
        unit = s.defaultUnit,
        strategy = s.spacingStrategy
    )

    // region Calculator input

    fun setMode(mode: MarkPointMode) = edit { it.copy(mode = mode) }

    fun setStart(text: String) = edit { it.copy(startText = sanitizeDecimal(text, allowNegative = true)) }

    fun setEnd(text: String) = edit { it.copy(endText = sanitizeDecimal(text, allowNegative = true)) }

    fun setMarks(text: String) = edit { it.copy(marksText = text.filter(Char::isDigit).take(4)) }

    fun setSpacing(text: String) = edit { it.copy(spacingText = sanitizeDecimal(text, allowNegative = true)) }

    fun toggleStartSign() = edit { it.copy(startText = toggleSign(it.startText)) }

    fun toggleEndSign() = edit { it.copy(endText = toggleSign(it.endText)) }

    fun setStrategy(strategy: SpacingStrategy) = edit { it.copy(strategy = strategy) }

    fun setAddEndpoint(enabled: Boolean) = edit { it.copy(addEndpoint = enabled) }

    /** Changes the unit while preserving the physical measurement (100 cm becomes 1 m, not 100 m). */
    fun setUnit(unit: MeasurementUnit) {
        val current = _state.value
        if (current.unit == unit) return
        fun convert(text: String): String {
            val value = NumberParser.parseDecimal(text) ?: return text
            return MeasurementFormatter.formatForInput(UnitConverter.convert(value, current.unit, unit))
        }
        val converted = current.copy(
            unit = unit,
            startText = convert(current.startText),
            endText = convert(current.endText),
            spacingText = convert(current.spacingText)
        )
        // The result is stored in millimeters, so an existing result is still exact; it is only re-displayed.
        _state.value = if (converted.result != null) converted else recalculateIfNeeded(converted)
    }

    fun calculate(): Boolean {
        _state.value = runCalculation(_state.value.copy(hasCalculated = true))
        return _state.value.result != null
    }

    fun reset() {
        _state.value = defaultState(currentSettings)
    }

    private fun edit(transform: (CalculatorState) -> CalculatorState) {
        _state.value = recalculateIfNeeded(transform(_state.value))
    }

    private fun recalculateIfNeeded(state: CalculatorState): CalculatorState =
        if (state.hasCalculated) runCalculation(state) else state.copy(result = null)

    private fun runCalculation(state: CalculatorState): CalculatorState {
        val validation = MarkPointValidator.validate(
            mode = state.mode,
            startText = state.startText,
            endText = state.endText,
            marksText = state.marksText,
            spacingText = state.spacingText,
            unit = state.unit,
            strategy = state.strategy,
            addEndpoint = state.addEndpoint
        )
        return when (validation) {
            is ValidationResult.Valid -> state.copy(
                errors = InputErrors(),
                result = MarkPointCalculator.calculate(validation.input)
            )
            is ValidationResult.Invalid -> state.copy(errors = validation.errors, result = null)
        }
    }

    // endregion

    // region Results

    fun summaryText(): String? = _state.value.result?.let {
        ResultTextBuilder.summary(it, _state.value.unit, currentSettings.decimalPlaces)
    }

    fun valuesText(): String? = _state.value.result?.let {
        ResultTextBuilder.compactValues(it, _state.value.unit, currentSettings.decimalPlaces)
    }

    fun save(): Boolean {
        val current = _state.value
        val result = current.result ?: return false
        val record = HistoryRecord.from(
            result = result,
            start = NumberParser.normalize(current.startText),
            end = NumberParser.normalize(current.endText),
            unit = current.unit,
            markCount = if (current.mode == MarkPointMode.MARKS) {
                NumberParser.parseInt(current.marksText) ?: result.spacedMarks.size
            } else {
                result.spacedMarks.size
            },
            spacing = NumberParser.normalize(current.spacingText),
            addEndpoint = current.addEndpoint,
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch { historyRepository.add(record) }
        return true
    }

    // endregion

    // region History

    fun restore(record: HistoryRecord) {
        val restored = _state.value.copy(
            initialized = true,
            mode = record.mode,
            startText = record.start,
            endText = record.end,
            unit = record.unit,
            marksText = if (record.mode == MarkPointMode.MARKS) record.markCount.toString() else _state.value.marksText,
            spacingText = if (record.mode == MarkPointMode.SPACING) record.spacing else _state.value.spacingText,
            strategy = record.strategy,
            addEndpoint = record.addEndpoint,
            hasCalculated = true
        )
        _state.value = runCalculation(restored)
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch { historyRepository.delete(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clear() }
    }

    // endregion

    // region Settings

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun resetSettings() {
        viewModelScope.launch { settingsRepository.reset() }
    }

    // endregion

    companion object {
        private const val MAX_INPUT_LENGTH = 16

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MarkPointApplication
                MarkPointViewModel(app.settingsRepository, app.historyRepository)
            }
        }

        /** Keeps digits, a single decimal point and an optional leading minus. Commas become points. */
        fun sanitizeDecimal(text: String, allowNegative: Boolean): String {
            val builder = StringBuilder()
            var hasPoint = false
            text.replace(',', '.').forEachIndexed { index, c ->
                when {
                    c.isDigit() -> builder.append(c)
                    c == '.' && !hasPoint -> {
                        hasPoint = true
                        builder.append(c)
                    }
                    c == '-' && allowNegative && index == 0 -> builder.append(c)
                }
            }
            return builder.toString().take(MAX_INPUT_LENGTH)
        }

        fun toggleSign(text: String): String = if (text.startsWith("-")) text.drop(1) else "-$text"
    }
}
