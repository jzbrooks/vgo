package com.jzbrooks.vgo.core.util.ir

import com.jzbrooks.vgo.core.graphic.Graphic
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

/**
 * Graphic debugging that dumps IR into a string.
 * Useful for debugging.
 *
 * Not intended for application use.
 */
fun Graphic.asIr(): String {
    val baos = ByteArrayOutputStream()
    val ps = PrintStream(baos, true, StandardCharsets.UTF_8)
    IrPrinter(ps).visit(this)
    return baos.toString(StandardCharsets.UTF_8)
}
