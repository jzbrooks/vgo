package com.jzbrooks.guacamole

import com.jzbrooks.guacamole.graphic.Graphic
import java.io.OutputStream

interface Writer {
    val options: Set<Option>
    fun write(graphic: Graphic, stream: OutputStream)
    sealed class Option {
        class Indent(val columns: Int) : Option()
    }
}