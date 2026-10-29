package com.coffeepeek.feature.shelf.data.scan.imaging

import kotlin.math.PI

// The same factors as java.lang.Math.toRadians/toDegrees, so results match the JVM probe bit for bit.
private const val DEGREES_TO_RADIANS = PI / 180
private const val RADIANS_TO_DEGREES = 180 / PI

internal fun radians(degrees: Double): Double = degrees * DEGREES_TO_RADIANS
internal fun degrees(radians: Double): Double = radians * RADIANS_TO_DEGREES
