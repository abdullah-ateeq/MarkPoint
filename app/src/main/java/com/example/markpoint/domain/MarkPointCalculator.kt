package com.example.markpoint.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Pure spacing math. Inputs are expected to be validated by [MarkPointValidator].
 * Values are in millimeters unless stated otherwise. Nothing is rounded here;
 * rounding happens only when formatting for display.
 */
object MarkPointCalculator {
    const val MAX_MARKS = 100
    const val MAX_SPACING_MARKS = 1000

    private val MC: MathContext = MathContext.DECIMAL128

    /** Calculates a validated input given in any unit. */
    fun calculate(input: MarkPointInput): MarkPointResult {
        val startMm = input.start.toMillimeters(input.unit)
        val endMm = input.end.toMillimeters(input.unit)
        return when (input.mode) {
            MarkPointMode.MARKS -> calculateEqualMarks(startMm, endMm, input.markCount)
            MarkPointMode.SPACING -> {
                val spacingMm = input.spacing.toMillimeters(input.unit)
                when (input.strategy) {
                    SpacingStrategy.EXACT_ENDPOINT -> calculateExactEndpointSpacing(startMm, endMm, spacingMm)
                    SpacingStrategy.FIXED_SPACING -> calculateFixedSpacing(startMm, endMm, spacingMm, input.addEndpoint)
                }
            }
        }
    }

    /** Signed interval: (end − start) ÷ markCount. Negative for descending measurements. */
    fun calculateInterval(start: BigDecimal, end: BigDecimal, markCount: Int): BigDecimal {
        require(markCount >= 1) { "markCount must be at least 1" }
        return (end - start).divide(BigDecimal(markCount), MC)
    }

    /**
     * Positions start + interval × n for n = 1..markCount.
     * Computed as start + (end − start) × n ÷ markCount so the final mark is exactly the end.
     */
    fun calculateMarkPositions(start: BigDecimal, end: BigDecimal, markCount: Int): List<MarkPosition> {
        require(markCount >= 1) { "markCount must be at least 1" }
        val distance = end - start
        val count = BigDecimal(markCount)
        return (1..markCount).map { n ->
            val position = start + distance.multiply(BigDecimal(n)).divide(count, MC)
            MarkPosition(index = n, positionMm = position, isEndpoint = false)
        }
    }

    fun calculateEqualMarks(start: BigDecimal, end: BigDecimal, markCount: Int): MarkPointResult {
        require(start.compareTo(end) != 0) { "start and end must differ" }
        return MarkPointResult(
            mode = MarkPointMode.MARKS,
            strategy = null,
            startMm = start,
            endMm = end,
            intervalMm = calculateInterval(start, end, markCount),
            marks = calculateMarkPositions(start, end, markCount),
            completeIntervals = markCount,
            remainingDistanceMm = BigDecimal.ZERO
        )
    }

    /** Number of complete intervals: floor(distance ÷ spacing). */
    fun calculateCompleteIntervals(distance: BigDecimal, spacing: BigDecimal): BigDecimal {
        require(spacing.signum() > 0) { "spacing must be positive" }
        return distance.abs().divideToIntegralValue(spacing)
    }

    fun calculateRemainingDistance(distance: BigDecimal, spacing: BigDecimal, completeIntervals: Int): BigDecimal =
        distance.abs() - spacing.multiply(BigDecimal(completeIntervals))

    /**
     * Keeps the requested spacing, moving from start toward end. Never places a mark beyond the end.
     * When [addEndpoint] is set and the spacing does not land on the end, the end is appended as a
     * separate endpoint (flagged [MarkPosition.isEndpoint]) that is not part of the equal spacing.
     */
    fun calculateFixedSpacing(
        start: BigDecimal,
        end: BigDecimal,
        spacing: BigDecimal,
        addEndpoint: Boolean = false
    ): MarkPointResult {
        require(start.compareTo(end) != 0) { "start and end must differ" }
        val distance = end - start
        val direction = BigDecimal(distance.signum())
        val intervals = calculateCompleteIntervals(distance, spacing).intValueExact()
        val step = spacing.multiply(direction)
        val marks = (1..intervals).map { n ->
            MarkPosition(index = n, positionMm = start + step.multiply(BigDecimal(n)), isEndpoint = false)
        }.toMutableList()
        val remaining = calculateRemainingDistance(distance, spacing, intervals)
        if (addEndpoint && remaining.signum() != 0) {
            marks += MarkPosition(index = intervals + 1, positionMm = end, isEndpoint = true)
        }
        return MarkPointResult(
            mode = MarkPointMode.SPACING,
            strategy = SpacingStrategy.FIXED_SPACING,
            startMm = start,
            endMm = end,
            intervalMm = step,
            marks = marks,
            completeIntervals = intervals,
            remainingDistanceMm = remaining,
            requestedSpacingMm = spacing
        )
    }

    /** Mark count used by the exact-endpoint strategy: distance ÷ spacing rounded to the nearest whole, at least 1. */
    fun exactEndpointMarkCount(distance: BigDecimal, spacing: BigDecimal): BigDecimal {
        require(spacing.signum() > 0) { "spacing must be positive" }
        val rounded = distance.abs().divide(spacing, MC).setScale(0, RoundingMode.HALF_UP)
        return rounded.max(BigDecimal.ONE)
    }

    /** Distributes marks evenly with the interval closest to the requested spacing that lands on the end. */
    fun calculateExactEndpointSpacing(start: BigDecimal, end: BigDecimal, spacing: BigDecimal): MarkPointResult {
        val count = exactEndpointMarkCount(end - start, spacing).intValueExact()
        return calculateEqualMarks(start, end, count).copy(
            mode = MarkPointMode.SPACING,
            strategy = SpacingStrategy.EXACT_ENDPOINT,
            requestedSpacingMm = spacing
        )
    }
}
