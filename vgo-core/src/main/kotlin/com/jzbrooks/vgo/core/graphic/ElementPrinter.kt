package com.jzbrooks.vgo.core.graphic

/**
 * Serializes geometry elements in a target document format.
 *
 * Implementations print geometry only.
 * Paint attributes are ignored in outputs.
 */
interface ElementPrinter {
    fun print(shape: Shape): String

    fun print(path: Path): String
}
