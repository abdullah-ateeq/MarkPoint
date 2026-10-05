package com.example.markpoint.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MarkPointCalculatorTest {

    private val cm = MeasurementUnit.CENTIMETER

    private fun marks(start: String, end: String, count: Int, unit: MeasurementUnit = cm) =
        MarkPointCalculator.calculate(
            MarkPointInput(MarkPointMode.MARKS, BigDecimal(start), BigDecimal(end), unit, count, BigDecimal.ZERO)
        )

    private fun spacing(
        start: String,
        end: String,
        spacing: String,
        strategy: SpacingStrategy,
        addEndpoint: Boolean = false,
        unit: MeasurementUnit = cm
    ) = MarkPointCalculator.calculate(
        MarkPointInput(MarkPointMode.SPACING, BigDecimal(start), BigDecimal(end), unit, 0, BigDecimal(spacing), strategy, addEndpoint)
    )

    /** Mark positions shown in [unit] at 2 decimals. */
    private fun MarkPointResult.positions(unit: MeasurementUnit = cm) =
        marks.map { MeasurementFormatter.formatNumber(it.positionMm.fromMillimeters(unit), 2) }

    private fun BigDecimal.inUnit(unit: MeasurementUnit = cm) = MeasurementFormatter.formatNumber(fromMillimeters(unit), 2)

    @Test
    fun equalMarks_zeroToHundred_fourMarks() {
        val result = marks("0", "100", 4)
        assertEquals(listOf("25.00", "50.00", "75.00", "100.00"), result.positions())
        assertEquals("25.00", result.intervalMm!!.inUnit())
        assertEquals("100.00", result.totalDistanceMm.inUnit())
    }

    @Test
    fun oneMark_isTheEndpoint() {
        assertEquals(listOf("100.00"), marks("0", "100", 1).positions())
    }

    @Test
    fun twoMarks() {
        assertEquals(listOf("50.00", "100.00"), marks("0", "100", 2).positions())
    }

    @Test
    fun multipleMarks_lastMarkIsExactlyTheEnd_evenWhenIntervalRepeats() {
        val result = marks("0", "100", 3)
        assertEquals(listOf("33.33", "66.67", "100.00"), result.positions())
        assertEquals(0, result.marks.last().positionMm.compareTo(BigDecimal("1000")))
        assertTrue(result.landsOnEndpoint)
    }

    @Test
    fun hundredMarks() {
        val result = marks("0", "100", 100)
        assertEquals(100, result.marks.size)
        assertEquals("1.00", result.positions().first())
        assertEquals("100.00", result.positions().last())
    }

    @Test
    fun startAtZero_marksAreMultiplesOfInterval() {
        val result = marks("0", "60", 6)
        assertEquals(listOf("10.00", "20.00", "30.00", "40.00", "50.00", "60.00"), result.positions())
    }

    @Test
    fun nonZeroStart() {
        val result = marks("10", "110", 5)
        assertEquals(listOf("30.00", "50.00", "70.00", "90.00", "110.00"), result.positions())
        assertEquals("20.00", result.intervalMm!!.inUnit())
    }

    @Test
    fun startOffset() {
        assertEquals(listOf("40.00", "65.00", "90.00", "115.00"), marks("15", "115", 4).positions())
    }

    @Test
    fun descending_preservesDirection() {
        val result = marks("100", "0", 4)
        assertEquals(listOf("75.00", "50.00", "25.00", "0.00"), result.positions())
        assertEquals("-25.00", result.intervalMm!!.inUnit())
        assertEquals("100.00", result.totalDistanceMm.inUnit())
    }

    @Test
    fun negativeMeasurements() {
        assertEquals(listOf("-25.00", "0.00", "25.00", "50.00"), marks("-50", "50", 4).positions())
    }

    @Test
    fun decimalValues() {
        val result = marks("0.5", "10.5", 4)
        assertEquals(listOf("3.00", "5.50", "8.00", "10.50"), result.positions())
        assertEquals("2.50", result.intervalMm!!.inUnit())
    }

    @Test
    fun fixedSpacing_nonExact_reportsRemaining() {
        val result = spacing("0", "100", "30", SpacingStrategy.FIXED_SPACING)
        assertEquals(listOf("30.00", "60.00", "90.00"), result.positions())
        assertEquals(3, result.completeIntervals)
        assertEquals("10.00", result.remainingDistanceMm!!.inUnit())
        assertFalse(result.landsOnEndpoint)
        assertNull(result.separateEndpoint)
    }

    @Test
    fun fixedSpacing_exactDivision_includesEndpoint() {
        val result = spacing("0", "100", "20", SpacingStrategy.FIXED_SPACING)
        assertEquals(listOf("20.00", "40.00", "60.00", "80.00", "100.00"), result.positions())
        assertEquals(5, result.completeIntervals)
        assertEquals(0, result.remainingDistanceMm!!.signum())
        assertTrue(result.landsOnEndpoint)
    }

    @Test
    fun fixedSpacing_addEndpoint_listsEndpointSeparately() {
        val result = spacing("0", "100", "30", SpacingStrategy.FIXED_SPACING, addEndpoint = true)
        assertEquals(listOf("30.00", "60.00", "90.00", "100.00"), result.positions())
        assertEquals(3, result.spacedMarks.size)
        assertTrue(result.marks.last().isEndpoint)
        assertEquals("100.00", result.separateEndpoint!!.positionMm.inUnit())
    }

    @Test
    fun fixedSpacing_addEndpoint_notDuplicatedWhenExact() {
        val result = spacing("0", "100", "20", SpacingStrategy.FIXED_SPACING, addEndpoint = true)
        assertEquals(5, result.marks.size)
        assertNull(result.separateEndpoint)
    }

    @Test
    fun fixedSpacing_descending_neverPassesEndpoint() {
        val result = spacing("100", "0", "30", SpacingStrategy.FIXED_SPACING)
        assertEquals(listOf("70.00", "40.00", "10.00"), result.positions())
        assertEquals("10.00", result.remainingDistanceMm!!.inUnit())
    }

    @Test
    fun remainingDistance() {
        val remaining = MarkPointCalculator.calculateRemainingDistance(BigDecimal("1000"), BigDecimal("300"), 3)
        assertEquals(0, remaining.compareTo(BigDecimal("100")))
        assertEquals(0, MarkPointCalculator.calculateCompleteIntervals(BigDecimal("-1000"), BigDecimal("300")).compareTo(BigDecimal(3)))
    }

    @Test
    fun exactEndpointSpacing_adjustsIntervalToLandOnEnd() {
        val result = spacing("0", "100", "30", SpacingStrategy.EXACT_ENDPOINT)
        assertEquals(listOf("33.33", "66.67", "100.00"), result.positions())
        assertTrue(result.landsOnEndpoint)
        assertTrue(result.spacingAdjusted)
    }

    @Test
    fun exactEndpointSpacing_exactDivision_keepsSpacing() {
        val result = spacing("0", "100", "20", SpacingStrategy.EXACT_ENDPOINT)
        assertEquals(5, result.marks.size)
        assertFalse(result.spacingAdjusted)
    }

    @Test
    fun inchUnits_calculatedInMillimeters() {
        val result = marks("0", "10", 4, MeasurementUnit.INCH)
        assertEquals(listOf("2.50", "5.00", "7.50", "10.00"), result.positions(MeasurementUnit.INCH))
        assertEquals(0, result.endMm.compareTo(BigDecimal("254.0")))
    }

    @Test
    fun veryLargeAndVerySmallDistances_doNotCrash() {
        val large = marks("0", "1000000000", 100, MeasurementUnit.METER)
        assertEquals(100, large.marks.size)
        val small = marks("0", "0.0001", 3, MeasurementUnit.MILLIMETER)
        assertEquals(3, small.marks.size)
        assertEquals("0.0000", MeasurementFormatter.formatNumber(small.marks.first().positionMm, 4))
    }
}
