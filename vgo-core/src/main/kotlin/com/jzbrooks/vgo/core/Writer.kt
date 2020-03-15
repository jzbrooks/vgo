package com.jzbrooks.vgo.core

import com.jzbrooks.vgo.core.graphic.Graphic
import java.io.OutputStream

interface Writer {
    val options: Set<Option>
    fun write(graphic: Graphic, stream: OutputStream)
    sealed class Option {
        class Indent(val columns: Int) : Option()
    }
}