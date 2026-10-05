package com.example.markpoint.domain

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.MathContext

@Serializable
enum class MeasurementUnit(
    val symbol: String,
    val spokenName: String,
    /** Exact number of millimeters in one unit. */
    val millimeters: BigDecimal
) {
    MILLIMETER("mm", "millimeters", BigDecimal.ONE),
    CENTIMETER("cm", "centimeters", BigDecimal.TEN),
    METER("m", "meters", BigDecimal("1000")),
    INCH("in", "inches", BigDecimal("25.4")),
    FOOT("ft", "feet", BigDecimal("304.8")),
    YARD("yd", "yards", BigDecimal("914.4"));
}

/** Centralized unit conversion. All calculations are performed in millimeters. */
object UnitConverter {
    val PRECISION: MathContext = MathContext.DECIMAL128

    fun toMillimeters(value: BigDecimal, unit: MeasurementUnit): BigDecimal =
        value.multiply(unit.millimeters)

    fun fromMillimeters(valueMm: BigDecimal, unit: MeasurementUnit): BigDecimal =
        valueMm.divide(unit.millimeters, PRECISION)

    fun convert(value: BigDecimal, from: MeasurementUnit, to: MeasurementUnit): BigDecimal =
        if (from == to) value else fromMillimeters(toMillimeters(value, from), to)
}

fun BigDecimal.toMillimeters(unit: MeasurementUnit): BigDecimal = UnitConverter.toMillimeters(this, unit)

fun BigDecimal.fromMillimeters(unit: MeasurementUnit): BigDecimal = UnitConverter.fromMillimeters(this, unit)
