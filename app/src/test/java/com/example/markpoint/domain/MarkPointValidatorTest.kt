package com.example.markpoint.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkPointValidatorTest {

    private fun validate(
        start: String = "0",
        end: String = "100",
        marks: String = "4",
        spacing: String = "10",
        mode: MarkPointMode = MarkPointMode.MARKS,
        strategy: SpacingStrategy = SpacingStrategy.FIXED_SPACING
    ) = MarkPointValidator.validate(mode, start, end, marks, spacing, MeasurementUnit.CENTIMETER, strategy, false)

    private fun errors(result: ValidationResult): InputErrors = (result as ValidationResult.Invalid).errors

    @Test
    fun validInput() {
        assertTrue(validate() is ValidationResult.Valid)
        assertTrue(validate(start = "-50", end = "50,5") is ValidationResult.Valid)
    }

    @Test
    fun zeroDistance() {
        assertEquals(ValidationMessages.SAME_START_END, errors(validate(start = "100", end = "100.0")).general)
    }

    @Test
    fun zeroSpacing() {
        assertEquals(ValidationMessages.SPACING_POSITIVE, errors(validate(mode = MarkPointMode.SPACING, spacing = "0")).spacing)
    }

    @Test
    fun negativeSpacing() {
        assertEquals(ValidationMessages.SPACING_POSITIVE, errors(validate(mode = MarkPointMode.SPACING, spacing = "-5")).spacing)
    }

    @Test
    fun invalidMarkCount() {
        assertEquals(ValidationMessages.MARKS_MIN, errors(validate(marks = "0")).marks)
        assertEquals(ValidationMessages.MARKS_MAX, errors(validate(marks = "101")).marks)
        assertEquals(ValidationMessages.MARKS_MAX, errors(validate(marks = "99999999999")).marks)
        assertEquals(ValidationMessages.MARKS_WHOLE, errors(validate(marks = "2.5")).marks)
        assertEquals(ValidationMessages.MARKS_EMPTY, errors(validate(marks = "")).marks)
    }

    @Test
    fun emptyInputs() {
        val e = errors(validate(start = "", end = " "))
        assertEquals(ValidationMessages.START_EMPTY, e.start)
        assertEquals(ValidationMessages.END_EMPTY, e.end)
        assertEquals(ValidationMessages.SPACING_EMPTY, errors(validate(mode = MarkPointMode.SPACING, spacing = "")).spacing)
    }

    @Test
    fun malformedNumbersAreRejectedSafely() {
        listOf("-", ".", "1.2.3", "1e5", "NaN", "Infinity", "--1", "abc").forEach {
            assertEquals("input '$it'", ValidationMessages.INVALID_NUMBER, errors(validate(start = it)).start)
        }
        assertNull(NumberParser.parseDecimal("1e999999999"))
    }

    @Test
    fun spacingLargerThanDistance() {
        assertEquals(ValidationMessages.SPACING_TOO_LARGE, errors(validate(mode = MarkPointMode.SPACING, spacing = "150")).general)
    }

    @Test
    fun spacingCreatingTooManyMarks() {
        assertEquals(
            ValidationMessages.SPACING_TOO_MANY,
            errors(validate(mode = MarkPointMode.SPACING, spacing = "0.001")).spacing
        )
    }
}
