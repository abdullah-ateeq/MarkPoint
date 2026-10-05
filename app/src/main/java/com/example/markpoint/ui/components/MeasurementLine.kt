package com.example.markpoint.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.markpoint.domain.MarkOrientation
import com.example.markpoint.domain.MarkPointResult
import com.example.markpoint.domain.MeasurementFormatter
import com.example.markpoint.domain.MeasurementUnit
import com.example.markpoint.domain.fromMillimeters
import java.math.BigDecimal
import java.math.MathContext

/** Above this many marks, dots are replaced by thin ticks and only non-overlapping labels are drawn. */
private const val DETAILED_MARK_LIMIT = 40

private data class VisualPoint(
    val fraction: Float,
    val label: String,
    val kind: Kind
) {
    enum class Kind { START, MARK, END_MARK, SEPARATE_ENDPOINT, END_CAP }
}

private data class VisualColors(
    val line: Color,
    val start: Color,
    val mark: Color,
    val endpoint: Color,
    val label: Color,
    val caption: Color
)

@Composable
fun MeasurementLine(
    result: MarkPointResult,
    unit: MeasurementUnit,
    decimals: Int,
    orientation: MarkOrientation,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    val scheme = MaterialTheme.colorScheme
    val colors = VisualColors(
        line = scheme.outline,
        start = scheme.onSurface,
        mark = scheme.primary,
        endpoint = scheme.tertiary,
        label = scheme.onSurface,
        caption = scheme.onSurfaceVariant
    )
    val points = remember(result, unit, decimals) { buildPoints(result, unit, decimals) }
    val description = remember(result, unit, decimals) {
        val start = MeasurementFormatter.spokenMeasurement(result.startMm, unit, decimals)
        val end = MeasurementFormatter.spokenMeasurement(result.endMm, unit, decimals)
        "Measurement line from $start to $end with ${result.spacedMarks.size} marked positions"
    }
    val markCount = result.spacedMarks.size

    val sizeModifier = when (orientation) {
        MarkOrientation.HORIZONTAL -> Modifier.fillMaxWidth().height(84.dp)
        MarkOrientation.VERTICAL -> Modifier.fillMaxWidth().height((markCount * 30 + 60).coerceIn(200, 440).dp)
    }

    Canvas(modifier.then(sizeModifier).semantics { contentDescription = description }) {
        when (orientation) {
            MarkOrientation.HORIZONTAL -> drawHorizontal(points, markCount, measurer, colors, result.remainingDistanceMm)
            MarkOrientation.VERTICAL -> drawVertical(points, markCount, measurer, colors, result.remainingDistanceMm)
        }
    }
}

private fun buildPoints(result: MarkPointResult, unit: MeasurementUnit, decimals: Int): List<VisualPoint> {
    val span = result.endMm - result.startMm
    fun fraction(mm: BigDecimal): Float =
        if (span.signum() == 0) 0f
        else (mm - result.startMm).divide(span, MathContext.DECIMAL64).toFloat().coerceIn(0f, 1f)
    fun label(mm: BigDecimal) = MeasurementFormatter.formatCompactNumber(mm.fromMillimeters(unit), decimals)

    val points = mutableListOf(VisualPoint(0f, label(result.startMm), VisualPoint.Kind.START))
    result.spacedMarks.forEach { mark ->
        val atEnd = mark.positionMm.compareTo(result.endMm) == 0
        points += VisualPoint(
            fraction(mark.positionMm),
            label(mark.positionMm),
            if (atEnd) VisualPoint.Kind.END_MARK else VisualPoint.Kind.MARK
        )
    }
    if (!result.landsOnEndpoint) {
        val kind = if (result.separateEndpoint != null) VisualPoint.Kind.SEPARATE_ENDPOINT else VisualPoint.Kind.END_CAP
        points += VisualPoint(1f, label(result.endMm), kind)
    }
    return points
}

/**
 * Chooses which labels to draw so none overlap: start and end first, then every other point
 * greedily in order. [extent] returns the occupied range of a label along the axis.
 */
private fun chooseLabels(points: List<VisualPoint>, extent: (Int) -> ClosedFloatingPointRange<Float>, gap: Float): Set<Int> {
    val chosen = mutableListOf<ClosedFloatingPointRange<Float>>()
    val result = mutableSetOf<Int>()
    fun tryAdd(i: Int) {
        val range = extent(i)
        val overlaps = chosen.any { range.start < it.endInclusive + gap && it.start < range.endInclusive + gap }
        if (!overlaps) {
            chosen += range
            result += i
        }
    }
    tryAdd(0)
    tryAdd(points.lastIndex)
    for (i in 1 until points.lastIndex) tryAdd(i)
    return result
}

private fun DrawScope.drawPointMarker(point: VisualPoint, center: Offset, detailed: Boolean, colors: VisualColors, vertical: Boolean) {
    val r = 1.dp.toPx()
    val tickHalf = if (detailed) 8 * r else 6 * r
    val tickStart = if (vertical) Offset(center.x - tickHalf, center.y) else Offset(center.x, center.y - tickHalf)
    val tickEnd = if (vertical) Offset(center.x + tickHalf, center.y) else Offset(center.x, center.y + tickHalf)
    when (point.kind) {
        VisualPoint.Kind.START -> {
            drawCircle(colors.start, radius = 6 * r, center = center, style = Stroke(width = 2 * r))
        }
        VisualPoint.Kind.MARK -> {
            drawLine(colors.mark, tickStart, tickEnd, strokeWidth = if (detailed) 2 * r else 1.5f * r, cap = StrokeCap.Round)
            if (detailed) drawCircle(colors.mark, radius = 4 * r, center = center)
        }
        VisualPoint.Kind.END_MARK -> {
            drawLine(colors.mark, tickStart, tickEnd, strokeWidth = 2 * r, cap = StrokeCap.Round)
            drawCircle(colors.mark, radius = 6.5f * r, center = center)
            drawCircle(colors.mark.copy(alpha = 0.25f), radius = 11 * r, center = center)
        }
        VisualPoint.Kind.SEPARATE_ENDPOINT -> {
            val d = 6 * r
            val diamond = Path().apply {
                moveTo(center.x, center.y - d)
                lineTo(center.x + d, center.y)
                lineTo(center.x, center.y + d)
                lineTo(center.x - d, center.y)
                close()
            }
            drawPath(diamond, colors.endpoint)
        }
        VisualPoint.Kind.END_CAP -> {
            drawLine(colors.start, tickStart, tickEnd, strokeWidth = 2 * r, cap = StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawTrack(from: Offset, to: Offset, lastMark: Offset?, hasRemainder: Boolean, colors: VisualColors) {
    val stroke = 2.dp.toPx()
    if (hasRemainder && lastMark != null) {
        drawLine(colors.line, from, lastMark, strokeWidth = stroke)
        drawLine(
            colors.line.copy(alpha = 0.6f), lastMark, to, strokeWidth = stroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        )
    } else {
        drawLine(colors.line, from, to, strokeWidth = stroke)
    }
}

private fun labelStyle(color: Color, emphasized: Boolean) = TextStyle(
    color = color,
    fontSize = 11.sp,
    fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
    fontFeatureSettings = "tnum"
)

private fun captionStyle(color: Color) = TextStyle(color = color, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold)

private fun measureLabels(points: List<VisualPoint>, measurer: TextMeasurer, colors: VisualColors): List<TextLayoutResult> =
    points.map { p ->
        val emphasized = p.kind != VisualPoint.Kind.MARK
        measurer.measure(p.label, labelStyle(if (p.kind == VisualPoint.Kind.SEPARATE_ENDPOINT) colors.endpoint else colors.label, emphasized))
    }

private fun DrawScope.drawHorizontal(
    points: List<VisualPoint>,
    markCount: Int,
    measurer: TextMeasurer,
    colors: VisualColors,
    remainder: BigDecimal?
) {
    val detailed = markCount <= DETAILED_MARK_LIMIT
    val edge = 24.dp.toPx()
    val trackY = size.height * 0.5f
    val left = edge
    val right = size.width - edge
    val width = (right - left).coerceAtLeast(1f)
    fun x(fraction: Float) = left + width * fraction

    val startCaption = measurer.measure("START", captionStyle(colors.caption))
    val endCaption = measurer.measure("END", captionStyle(colors.caption))
    drawText(startCaption, topLeft = Offset((left - startCaption.size.width / 2f).coerceAtLeast(0f), 0f))
    drawText(endCaption, topLeft = Offset((right - endCaption.size.width / 2f).coerceAtMost(size.width - endCaption.size.width), 0f))

    val lastMark = points.lastOrNull { it.kind == VisualPoint.Kind.MARK || it.kind == VisualPoint.Kind.END_MARK }
    drawTrack(
        Offset(left, trackY), Offset(right, trackY),
        lastMark?.let { Offset(x(it.fraction), trackY) },
        hasRemainder = remainder != null && remainder.signum() != 0, colors = colors
    )
    points.forEach { drawPointMarker(it, Offset(x(it.fraction), trackY), detailed, colors, vertical = false) }

    val layouts = measureLabels(points, measurer, colors)
    fun labelLeft(i: Int): Float {
        val w = layouts[i].size.width.toFloat()
        return (x(points[i].fraction) - w / 2f).coerceIn(0f, (size.width - w).coerceAtLeast(0f))
    }
    val shown = chooseLabels(points, { i -> labelLeft(i)..(labelLeft(i) + layouts[i].size.width) }, gap = 6.dp.toPx())
    val labelTop = trackY + 16.dp.toPx()
    shown.forEach { i -> drawText(layouts[i], topLeft = Offset(labelLeft(i), labelTop)) }
}

private fun DrawScope.drawVertical(
    points: List<VisualPoint>,
    markCount: Int,
    measurer: TextMeasurer,
    colors: VisualColors,
    remainder: BigDecimal?
) {
    val detailed = markCount <= DETAILED_MARK_LIMIT
    val edge = 16.dp.toPx()
    val trackX = 72.dp.toPx()
    val top = edge
    val bottom = size.height - edge
    val height = (bottom - top).coerceAtLeast(1f)
    fun y(fraction: Float) = top + height * fraction

    val startCaption = measurer.measure("START", captionStyle(colors.caption))
    val endCaption = measurer.measure("END", captionStyle(colors.caption))
    drawText(startCaption, topLeft = Offset(trackX - 16.dp.toPx() - startCaption.size.width, top - startCaption.size.height / 2f))
    drawText(endCaption, topLeft = Offset(trackX - 16.dp.toPx() - endCaption.size.width, bottom - endCaption.size.height / 2f))

    val lastMark = points.lastOrNull { it.kind == VisualPoint.Kind.MARK || it.kind == VisualPoint.Kind.END_MARK }
    drawTrack(
        Offset(trackX, top), Offset(trackX, bottom),
        lastMark?.let { Offset(trackX, y(it.fraction)) },
        hasRemainder = remainder != null && remainder.signum() != 0, colors = colors
    )
    points.forEach { drawPointMarker(it, Offset(trackX, y(it.fraction)), detailed, colors, vertical = true) }

    val layouts = measureLabels(points, measurer, colors)
    fun labelTop(i: Int): Float {
        val h = layouts[i].size.height.toFloat()
        return (y(points[i].fraction) - h / 2f).coerceIn(0f, (size.height - h).coerceAtLeast(0f))
    }
    val shown = chooseLabels(points, { i -> labelTop(i)..(labelTop(i) + layouts[i].size.height) }, gap = 2.dp.toPx())
    val labelX = trackX + 20.dp.toPx()
    shown.forEach { i -> drawText(layouts[i], topLeft = Offset(labelX, labelTop(i))) }
}
