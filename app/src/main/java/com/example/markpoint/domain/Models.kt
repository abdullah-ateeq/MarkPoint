package com.example.markpoint.domain

import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
enum class MarkPointMode {
    MARKS,
    SPACING
}

/** How spacing mode treats a spacing that does not divide the distance evenly. */
@Serializable
enum class SpacingStrategy {
    /** Distribute marks evenly so the final mark lands exactly on the endpoint. */
    EXACT_ENDPOINT,

    /** Keep the requested spacing and report the remaining distance. */
    FIXED_SPACING
}

enum class MarkOrientation {
    HORIZONTAL,
    VERTICAL
}

data class MarkPointInput(
    val mode: MarkPointMode,
    val start: BigDecimal,
    val end: BigDecimal,
    val unit: MeasurementUnit,
    val markCount: Int,
    val spacing: BigDecimal,
    val strategy: SpacingStrategy = SpacingStrategy.EXACT_ENDPOINT,
    val addEndpoint: Boolean = false
)

/**
 * A calculated position.
 *
 * [isEndpoint] is true only for an endpoint that is listed separately because it is
 * NOT part of the equal spacing (fixed spacing that does not divide evenly).
 */
data class MarkPosition(
    val index: Int,
    val positionMm: BigDecimal,
    val isEndpoint: Boolean
)

data class MarkPointResult(
    val mode: MarkPointMode,
    val strategy: SpacingStrategy?,
    val startMm: BigDecimal,
    val endMm: BigDecimal,
    val intervalMm: BigDecimal?,
    val marks: List<MarkPosition>,
    val completeIntervals: Int?,
    val remainingDistanceMm: BigDecimal?,
    val requestedSpacingMm: BigDecimal? = null
) {
    val totalDistanceMm: BigDecimal get() = (endMm - startMm).abs()

    /** Marks that belong to the equal spacing (excludes a separately listed endpoint). */
    val spacedMarks: List<MarkPosition> get() = marks.filterNot { it.isEndpoint }

    val separateEndpoint: MarkPosition? get() = marks.firstOrNull { it.isEndpoint }

    /** True when the last equally spaced mark coincides with the endpoint. */
    val landsOnEndpoint: Boolean
        get() = spacedMarks.lastOrNull()?.positionMm?.compareTo(endMm) == 0

    /** True when the requested spacing had to be adjusted to land on the endpoint. */
    val spacingAdjusted: Boolean
        get() = strategy == SpacingStrategy.EXACT_ENDPOINT &&
            requestedSpacingMm != null && intervalMm != null &&
            requestedSpacingMm.compareTo(intervalMm.abs()) != 0
}
