package com.jzbrooks.vgo.core.graphic

/**
 * Serializes geometry elements in a target document format.
 *
 * Implementations print geometry only. Paint attributes are equal
 * across the representations these strings are used to compare.
 */
interface ShapePrinter {
    fun print(shape: Shape): String

    fun print(path: Path): String
}
