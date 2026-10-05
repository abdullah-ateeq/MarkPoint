package com.example.markpoint.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class UnitAndFormatTest {

    private fun assertMm(expected: String, value: String, unit: MeasurementUnit) =
        assertEquals(0, BigDecimal(value).toMillimeters(unit).compareTo(BigDecimal(expected)))

    @Test
    fun millimeterConversion() {
        assertMm("12.5", "12.5", MeasurementUnit.MILLIMETER)
        assertEquals(0, BigDecimal("7").fromMillimeters(MeasurementUnit.MILLIMETER).compareTo(BigDecimal("7")))
    }

    @Test
    fun centimeterConversion() {
        assertMm("1000", "100", MeasurementUnit.CENTIMETER)
        assertEquals(0, BigDecimal("250").fromMillimeters(MeasurementUnit.CENTIMETER).compareTo(BigDecimal("25")))
    }

    @Test
    fun meterConversion() {
        assertMm("1500", "1.5", MeasurementUnit.METER)
        assertEquals(0, BigDecimal("1000").fromMillimeters(MeasurementUnit.METER).compareTo(BigDecimal.ONE))
    }

    @Test
    fun inchConversion() {
        assertMm("25.4", "1", MeasurementUnit.INCH)
        assertEquals(0, BigDecimal("127").fromMillimeters(MeasurementUnit.INCH).compareTo(BigDecimal("5")))
    }

    @Test
    fun feetConversion() {
        assertMm("304.8", "1", MeasurementUnit.FOOT)
        assertEquals(0, BigDecimal("914.4").fromMillimeters(MeasurementUnit.FOOT).compareTo(BigDecimal("3")))
    }

    @Test
    fun yardConversion() {
        assertMm("914.4", "1", MeasurementUnit.YARD)
        assertEquals(0, BigDecimal("1828.8").fromMillimeters(MeasurementUnit.YARD).compareTo(BigDecimal("2")))
    }

    @Test
    fun unitChangePreservesPhysicalLength() {
        val meters = UnitConverter.convert(BigDecimal("100"), MeasurementUnit.CENTIMETER, MeasurementUnit.METER)
        assertEquals("1.00", MeasurementFormatter.formatNumber(meters, 2))
        val inches = UnitConverter.convert(BigDecimal("1"), MeasurementUnit.FOOT, MeasurementUnit.INCH)
        assertEquals("12", MeasurementFormatter.formatForInput(inches))
    }

    @Test
    fun resultFormatting() {
        assertEquals("25.00 cm", MeasurementFormatter.formatMeasurement(BigDecimal("250"), MeasurementUnit.CENTIMETER, 2))
        assertEquals("2.50 cm", MeasurementFormatter.formatMeasurement(BigDecimal("25"), MeasurementUnit.CENTIMETER, 2))
        assertEquals("1.25 m", MeasurementFormatter.formatMeasurement(BigDecimal("1250"), MeasurementUnit.METER, 2))
        assertEquals("4.92 in", MeasurementFormatter.formatMeasurement(BigDecimal("125"), MeasurementUnit.INCH, 2))
        assertEquals("33.3 cm", MeasurementFormatter.formatMeasurement(BigDecimal("333.333333"), MeasurementUnit.CENTIMETER, 1))
        assertEquals("33.3333 cm", MeasurementFormatter.formatMeasurement(BigDecimal("333.333333"), MeasurementUnit.CENTIMETER, 4))
    }

    @Test
    fun formattingNeverShowsNegativeZeroOrNoise() {
        assertEquals("0.00", MeasurementFormatter.formatNumber(BigDecimal("-0.0001"), 2))
        assertEquals("25.00", MeasurementFormatter.formatNumber(BigDecimal("25.000000001"), 2))
        assertEquals("25", MeasurementFormatter.formatCompactNumber(BigDecimal("25.000000001"), 2))
        assertEquals("0", MeasurementFormatter.formatCompactNumber(BigDecimal("0.000"), 2))
    }

    @Test
    fun copySummary() {
        val result = MarkPointCalculator.calculateEqualMarks(BigDecimal.ZERO, BigDecimal("1000"), 4)
        val summary = ResultTextBuilder.summary(result, MeasurementUnit.CENTIMETER, 2)
        assertEquals(
            """
            MarkPoint
            Mode: Equal marks
            Start: 0 cm
            End: 100 cm
            Marks: 4
            Interval: 25.00 cm

            Mark 1: 25.00 cm
            Mark 2: 50.00 cm
            Mark 3: 75.00 cm
            Mark 4: 100.00 cm
            """.trimIndent(),
            summary
        )
        assertEquals(
            "25.00 cm\n50.00 cm\n75.00 cm\n100.00 cm",
            ResultTextBuilder.compactValues(result, MeasurementUnit.CENTIMETER, 2)
        )
    }

    @Test
    fun breakdownFormulaAndSteps() {
        val result = MarkPointCalculator.calculateEqualMarks(BigDecimal.ZERO, BigDecimal("1000"), 4)
        val breakdown = ResultTextBuilder.breakdown(result, MeasurementUnit.CENTIMETER, 2)
        assertEquals("(100 − 0) ÷ 4 = 25 cm", breakdown.formula.last())
        assertEquals(listOf("0 + 25 = 25 cm", "0 + 50 = 50 cm", "0 + 75 = 75 cm", "0 + 100 = 100 cm"), breakdown.steps)

        val descending = MarkPointCalculator.calculateEqualMarks(BigDecimal("1000"), BigDecimal.ZERO, 4)
        assertEquals("100 − 25 = 75 cm", ResultTextBuilder.breakdown(descending, MeasurementUnit.CENTIMETER, 2).steps.first())
    }
}
