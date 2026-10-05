package com.example.markpoint.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.markpoint.domain.MarkOrientation
import com.example.markpoint.domain.MarkPointCalculator
import com.example.markpoint.domain.MarkPointMode
import com.example.markpoint.domain.MeasurementFormatter
import com.example.markpoint.domain.MeasurementUnit
import com.example.markpoint.domain.SpacingStrategy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { LIGHT, DARK, SYSTEM }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val defaultMode: MarkPointMode = MarkPointMode.MARKS,
    val defaultUnit: MeasurementUnit = MeasurementUnit.CENTIMETER,
    val defaultStart: String = "0",
    val defaultEnd: String = "100",
    val defaultMarks: Int = 4,
    val defaultSpacing: String = "10",
    val spacingStrategy: SpacingStrategy = SpacingStrategy.EXACT_ENDPOINT,
    val decimalPlaces: Int = 2,
    val orientation: MarkOrientation = MarkOrientation.HORIZONTAL,
    val showFormula: Boolean = true,
    val showSteps: Boolean = true,
    val showVisual: Boolean = true,
    val haptics: Boolean = true
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_MODE = stringPreferencesKey("default_mode")
        val DEFAULT_UNIT = stringPreferencesKey("default_unit")
        val DEFAULT_START = stringPreferencesKey("default_start")
        val DEFAULT_END = stringPreferencesKey("default_end")
        val DEFAULT_MARKS = intPreferencesKey("default_marks")
        val DEFAULT_SPACING = stringPreferencesKey("default_spacing")
        val SPACING_STRATEGY = stringPreferencesKey("spacing_strategy")
        val DECIMALS = intPreferencesKey("decimal_places")
        val ORIENTATION = stringPreferencesKey("orientation")
        val SHOW_FORMULA = booleanPreferencesKey("show_formula")
        val SHOW_STEPS = booleanPreferencesKey("show_steps")
        val SHOW_VISUAL = booleanPreferencesKey("show_visual")
        val HAPTICS = booleanPreferencesKey("haptics")

        val ALL = listOf(
            THEME, DYNAMIC_COLOR, DEFAULT_MODE, DEFAULT_UNIT, DEFAULT_START, DEFAULT_END, DEFAULT_MARKS,
            DEFAULT_SPACING, SPACING_STRATEGY, DECIMALS, ORIENTATION, SHOW_FORMULA, SHOW_STEPS, SHOW_VISUAL, HAPTICS
        )
    }

    val settings: Flow<AppSettings> = dataStore.data.map { it.toSettings() }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            themeMode = enumOrDefault(this[Keys.THEME], d.themeMode),
            dynamicColor = this[Keys.DYNAMIC_COLOR] ?: d.dynamicColor,
            defaultMode = enumOrDefault(this[Keys.DEFAULT_MODE], d.defaultMode),
            defaultUnit = enumOrDefault(this[Keys.DEFAULT_UNIT], d.defaultUnit),
            defaultStart = this[Keys.DEFAULT_START] ?: d.defaultStart,
            defaultEnd = this[Keys.DEFAULT_END] ?: d.defaultEnd,
            defaultMarks = (this[Keys.DEFAULT_MARKS] ?: d.defaultMarks).coerceIn(1, MarkPointCalculator.MAX_MARKS),
            defaultSpacing = this[Keys.DEFAULT_SPACING] ?: d.defaultSpacing,
            spacingStrategy = enumOrDefault(this[Keys.SPACING_STRATEGY], d.spacingStrategy),
            decimalPlaces = (this[Keys.DECIMALS] ?: d.decimalPlaces)
                .coerceIn(MeasurementFormatter.MIN_DECIMALS, MeasurementFormatter.MAX_DECIMALS),
            orientation = enumOrDefault(this[Keys.ORIENTATION], d.orientation),
            showFormula = this[Keys.SHOW_FORMULA] ?: d.showFormula,
            showSteps = this[Keys.SHOW_STEPS] ?: d.showSteps,
            showVisual = this[Keys.SHOW_VISUAL] ?: d.showVisual,
            haptics = this[Keys.HAPTICS] ?: d.haptics
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[Keys.THEME] = s.themeMode.name
            prefs[Keys.DYNAMIC_COLOR] = s.dynamicColor
            prefs[Keys.DEFAULT_MODE] = s.defaultMode.name
            prefs[Keys.DEFAULT_UNIT] = s.defaultUnit.name
            prefs[Keys.DEFAULT_START] = s.defaultStart
            prefs[Keys.DEFAULT_END] = s.defaultEnd
            prefs[Keys.DEFAULT_MARKS] = s.defaultMarks
            prefs[Keys.DEFAULT_SPACING] = s.defaultSpacing
            prefs[Keys.SPACING_STRATEGY] = s.spacingStrategy.name
            prefs[Keys.DECIMALS] = s.decimalPlaces
            prefs[Keys.ORIENTATION] = s.orientation.name
            prefs[Keys.SHOW_FORMULA] = s.showFormula
            prefs[Keys.SHOW_STEPS] = s.showSteps
            prefs[Keys.SHOW_VISUAL] = s.showVisual
            prefs[Keys.HAPTICS] = s.haptics
        }
    }

    suspend fun reset() {
        dataStore.edit { prefs -> Keys.ALL.forEach { prefs.remove(it) } }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}
