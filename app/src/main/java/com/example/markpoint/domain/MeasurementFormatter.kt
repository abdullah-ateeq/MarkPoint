package com.example.markpoint.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** Display formatting. This is the only place where values are rounded. */
object MeasurementFormatter {
    const val MIN_DECIMALS = 1
    const val MAX_DECIMALS = 4

    /** Fixed-precision number, e.g. 25.00. Never shows "-0.00". */
    fun formatNumber(value: BigDecimal, decimals: Int): String {
        val scaled = value.setScale(decimals.coerceIn(0, MAX_DECIMALS), RoundingMode.HALF_UP)
        return (if (scaled.signum() == 0) scaled.abs() else scaled).toPlainString()
    }

    /** Rounded to [decimals], then trailing zeros removed, e.g. 25 or 33.33. */
    fun formatCompactNumber(value: BigDecimal, decimals: Int): String {
        val scaled = value.setScale(decimals.coerceIn(0, MAX_DECIMALS), RoundingMode.HALF_UP)
        if (scaled.signum() == 0) return "0"
        return scaled.stripTrailingZeros().toPlainString()
    }

    /** A millimeter value shown in [unit] with [decimals] places, e.g. "25.00 cm". */
    fun formatMeasurement(valueMm: BigDecimal, unit: MeasurementUnit, decimals: Int): String =
        "${formatNumber(valueMm.fromMillimeters(unit), decimals)} ${unit.symbol}"

    fun formatCompactMeasurement(valueMm: BigDecimal, unit: MeasurementUnit, decimals: Int): String =
        "${formatCompactNumber(valueMm.fromMillimeters(unit), decimals)} ${unit.symbol}"

    /** Spoken form for accessibility, e.g. "25.00 centimeters". */
    fun spokenMeasurement(valueMm: BigDecimal, unit: MeasurementUnit, decimals: Int): String =
        "${formatNumber(valueMm.fromMillimeters(unit), decimals)} ${unit.spokenName}"

    /** Text placed back into an input field after a unit change; keeps enough precision to round-trip. */
    fun formatForInput(value: BigDecimal): String {
        val scaled = value.setScale(6, RoundingMode.HALF_UP)
        if (scaled.signum() == 0) return "0"
        return scaled.stripTrailingZeros().toPlainString()
    }
}

data class CalculationBreakdown(
    val formula: List<String>,
    val steps: List<String>
)

/** Builds the human-readable texts for a result: copy summaries and the formula explanation. */
object ResultTextBuilder {

    fun modeTitle(result: MarkPointResult): String = when {
        result.mode == MarkPointMode.MARKS -> "Equal marks"
        result.strategy == SpacingStrategy.FIXED_SPACING -> "Fixed spacing"
        else -> "Spacing · exact endpoint"
    }

    fun markLabel(mark: MarkPosition): String = if (mark.isEndpoint) "Endpoint" else "Mark ${mark.index}"

    fun summary(result: MarkPointResult, unit: MeasurementUnit, decimals: Int): String {
        fun m(value: BigDecimal) = MeasurementFormatter.formatMeasurement(value, unit, decimals)
        fun c(value: BigDecimal) = MeasurementFormatter.formatCompactMeasurement(value, unit, MeasurementFormatter.MAX_DECIMALS)
        return buildString {
            appendLine("MarkPoint")
            appendLine("Mode: ${modeTitle(result)}")
            appendLine("Start: ${c(result.startMm)}")
            appendLine("End: ${c(result.endMm)}")
            appendLine("Marks: ${result.spacedMarks.size}")
            result.requestedSpacingMm?.let { appendLine("Requested spacing: ${c(it)}") }
            result.intervalMm?.let { appendLine("Interval: ${m(it.abs())}") }
            if (result.mode == MarkPointMode.SPACING && result.strategy == SpacingStrategy.FIXED_SPACING) {
                result.remainingDistanceMm?.let { appendLine("Remaining: ${m(it)}") }
            }
            appendLine()
            result.marks.forEach { appendLine("${markLabel(it)}: ${m(it.positionMm)}") }
        }.trimEnd()
    }

    fun compactValues(result: MarkPointResult, unit: MeasurementUnit, decimals: Int): String =
        result.marks.joinToString("\n") { MeasurementFormatter.formatMeasurement(it.positionMm, unit, decimals) }

    fun breakdown(result: MarkPointResult, unit: MeasurementUnit, decimals: Int): CalculationBreakdown {
        fun n(valueMm: BigDecimal) = operand(MeasurementFormatter.formatCompactNumber(valueMm.fromMillimeters(unit), decimals))
        fun first(valueMm: BigDecimal) = MeasurementFormatter.formatCompactNumber(valueMm.fromMillimeters(unit), decimals)
        val symbol = unit.symbol
        val interval = result.intervalMm ?: BigDecimal.ZERO
        val count = result.spacedMarks.size
        val distance = result.totalDistanceMm

        val formula = mutableListOf<String>()
        if (result.strategy == SpacingStrategy.FIXED_SPACING) {
            val spacing = result.requestedSpacingMm ?: interval.abs()
            formula += "Intervals = floor(Distance ÷ Spacing)"
            formula += "floor(${first(distance)} ÷ ${n(spacing)}) = ${result.completeIntervals ?: count}"
            result.remainingDistanceMm?.let {
                formula += "Remaining = ${first(distance)} − ${result.completeIntervals ?: count} × ${n(spacing)} = ${first(it)} $symbol"
            }
        } else {
            if (result.strategy == SpacingStrategy.EXACT_ENDPOINT) {
                val spacing = result.requestedSpacingMm ?: interval.abs()
                formula += "Marks = round(Distance ÷ Spacing)"
                formula += "round(${first(distance)} ÷ ${n(spacing)}) = $count"
            }
            formula += "Interval = (End − Start) ÷ Marks"
            formula += "(${first(result.endMm)} − ${n(result.startMm)}) ÷ $count = ${first(interval)} $symbol"
        }

        val steps = result.spacedMarks.map { mark ->
            val offset = interval.multiply(BigDecimal(mark.index))
            val operator = if (offset.signum() < 0) "−" else "+"
            "${first(result.startMm)} $operator ${first(offset.abs())} = ${first(mark.positionMm)} $symbol"
        }
        return CalculationBreakdown(formula, steps)
    }

    /** Wraps negative numbers in parentheses when used after an operator. */
    private fun operand(number: String): String = if (number.startsWith("-")) "($number)" else number
}
