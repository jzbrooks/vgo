package com.jzbrooks.vgo.core.util.math

/** Coordinate system is SVGs. Origin is top-left. Coordinates lower on the screen are 'higher'. */
data class Rectangle(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

infix fun Rectangle.intersects(rectangle: Rectangle): Boolean =
    !(left > rectangle.right || right < rectangle.left || top < rectangle.bottom || bottom > rectangle.top)
