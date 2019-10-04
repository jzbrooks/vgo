package com.jzbrooks.guacamole

import com.jzbrooks.guacamole.core.graphic.Graphic
import java.io.OutputStream

interface Writer {
    val options: Set<Option>
    fun write(graphic: Graphic, stream: OutputStream)
    sealed class Option {
        class Indent(val columns: Int) : Option()
    }
}