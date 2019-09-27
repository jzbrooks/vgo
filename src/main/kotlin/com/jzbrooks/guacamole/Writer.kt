package com.jzbrooks.guacamole

import com.jzbrooks.guacamole.graphic.Graphic
import java.io.OutputStream

interface Writer {
    val options: Set<Option>
    fun write(graphic: Graphic, stream: OutputStream)
    enum class Option {
        INDENT
    }
}