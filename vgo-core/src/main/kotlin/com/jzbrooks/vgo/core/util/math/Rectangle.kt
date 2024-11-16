package com.jzbrooks.vgo.core.util.math

/** Coordinate system is SVGs. Origin is top-left. Coordinates lower on the screen are 'higher'. */
data class Rectangle(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)
