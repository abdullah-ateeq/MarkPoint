package com.example.markpoint.domain

import java.math.BigDecimal

data class InputErrors(
    val start: String? = null,
    val end: String? = null,
    val marks: String? = null,
    val spacing: String? = null,
    val general: String? = null
) {
    val hasAny: Boolean get() = listOf(start, end, marks, spacing, general).any { it != null }
}

sealed interface ValidationResult {
    data class Valid(val input: MarkPointInput) : ValidationResult
    data class Invalid(val errors: InputErrors) : ValidationResult
}

object ValidationMessages {
    const val START_EMPTY = "Enter a start measurement."
    const val END_EMPTY = "Enter an end measurement."
    const val MARKS_EMPTY = "Enter the number of marks."
    const val SPACING_EMPTY = "Enter a spacing value."
    const val INVALID_NUMBER = "Enter a valid number."
    const val TOO_LARGE = "Value is too large."
    const val MARKS_WHOLE = "Enter a whole number of marks."
    const val MARKS_MIN = "Enter at least one mark."
    const val MARKS_MAX = "Maximum ${MarkPointCalculator.MAX_MARKS} marks."
    const val SPACING_POSITIVE = "Spacing must be greater than zero."
    const val SAME_START_END = "Start and end measurements must be different."
    const val SPACING_TOO_LARGE = "Spacing is larger than the distance between start and end."
    const val SPACING_TOO_MANY = "This spacing creates more than ${MarkPointCalculator.MAX_SPACING_MARKS} marks. Use a larger spacing."
}

/** Safe numeric parsing. Never throws; returns null for anything that is not a plain decimal number. */
object NumberParser {
    /** Largest accepted magnitude for an entered value, in the entered unit. */
    val MAX_MAGNITUDE: BigDecimal = BigDecimal("1000000000")
    private val DECIMAL_PATTERN = Regex("^-?(\\d+\\.?\\d*|\\.\\d+)$")
    private val INTEGER_PATTERN = Regex("^-?\\d+$")

    fun normalize(text: String): String = text.trim().replace(',', '.')

    fun parseDecimal(text: String): BigDecimal? {
        val normalized = normalize(text)
        if (normalized.length > 24 || !DECIMAL_PATTERN.matches(normalized)) return null
        return try {
            BigDecimal(normalized)
        } catch (_: NumberFormatException) {
            null
        }
    }

    fun parseInt(text: String): Int? {
        val normalized = normalize(text)
        if (normalized.length > 9 || !INTEGER_PATTERN.matches(normalized)) return null
        return normalized.toIntOrNull()
    }
}

object MarkPointValidator {

    fun validate(
        mode: MarkPointMode,
        startText: String,
        endText: String,
        marksText: String,
        spacingText: String,
        unit: MeasurementUnit,
        strategy: SpacingStrategy,
        addEndpoint: Boolean
    ): ValidationResult {
        val (start, startError) = parseMeasurement(startText, ValidationMessages.START_EMPTY)
        val (end, endError) = parseMeasurement(endText, ValidationMessages.END_EMPTY)

        var marksError: String? = null
        var markCount = 0
        var spacingError: String? = null
        var spacing = BigDecimal.ZERO

        when (mode) {
            MarkPointMode.MARKS -> {
                val result = validateMarkCount(marksText)
                markCount = result.first ?: 0
                marksError = result.second
            }
            MarkPointMode.SPACING -> {
                val result = validateSpacing(spacingText)
                spacing = result.first ?: BigDecimal.ZERO
                spacingError = result.second
            }
        }

        var generalError: String? = null
        if (start != null && end != null && start.compareTo(end) == 0) {
            generalError = ValidationMessages.SAME_START_END
        }

        if (mode == MarkPointMode.SPACING && start != null && end != null && generalError == null && spacingError == null) {
            val distance = (end - start).abs()
            if (spacing > distance) {
                generalError = ValidationMessages.SPACING_TOO_LARGE
            } else {
                val count = when (strategy) {
                    SpacingStrategy.FIXED_SPACING -> MarkPointCalculator.calculateCompleteIntervals(distance, spacing)
                    SpacingStrategy.EXACT_ENDPOINT -> MarkPointCalculator.exactEndpointMarkCount(distance, spacing)
                }
                if (count > BigDecimal(MarkPointCalculator.MAX_SPACING_MARKS)) {
                    spacingError = ValidationMessages.SPACING_TOO_MANY
                }
            }
        }

        val errors = InputErrors(startError, endError, marksError, spacingError, generalError)
        if (errors.hasAny || start == null || end == null) return ValidationResult.Invalid(errors)

        return ValidationResult.Valid(
            MarkPointInput(
                mode = mode,
                start = start,
                end = end,
                unit = unit,
                markCount = markCount,
                spacing = spacing,
                strategy = strategy,
                addEndpoint = addEndpoint
            )
        )
    }

    fun parseMeasurement(text: String, emptyMessage: String): Pair<BigDecimal?, String?> {
        if (text.isBlank()) return null to emptyMessage
        val value = NumberParser.parseDecimal(text) ?: return null to ValidationMessages.INVALID_NUMBER
        if (value.abs() > NumberParser.MAX_MAGNITUDE) return null to ValidationMessages.TOO_LARGE
        return value to null
    }

    fun validateMarkCount(text: String): Pair<Int?, String?> {
        if (text.isBlank()) return null to ValidationMessages.MARKS_EMPTY
        val normalized = NumberParser.normalize(text)
        val count = NumberParser.parseInt(normalized)
        if (count == null) {
            // A decimal such as "2.5" is not a valid count; an oversized integer exceeds the maximum.
            return if (Regex("^\\d{10,}$").matches(normalized)) null to ValidationMessages.MARKS_MAX
            else null to ValidationMessages.MARKS_WHOLE
        }
        if (count < 1) return null to ValidationMessages.MARKS_MIN
        if (count > MarkPointCalculator.MAX_MARKS) return null to ValidationMessages.MARKS_MAX
        return count to null
    }

    fun validateSpacing(text: String): Pair<BigDecimal?, String?> {
        if (text.isBlank()) return null to ValidationMessages.SPACING_EMPTY
        val value = NumberParser.parseDecimal(text) ?: return null to ValidationMessages.INVALID_NUMBER
        if (value.signum() <= 0) return null to ValidationMessages.SPACING_POSITIVE
        if (value > NumberParser.MAX_MAGNITUDE) return null to ValidationMessages.TOO_LARGE
        return value to null
    }
}
