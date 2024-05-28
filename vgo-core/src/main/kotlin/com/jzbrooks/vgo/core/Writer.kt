package com.jzbrooks.vgo.core

import com.jzbrooks.vgo.core.graphic.Graphic
import java.io.OutputStream

interface Writer<in T : Graphic> {
    val options: Set<Option>

    fun write(
        graphic: T,
        stream: OutputStream,
    )

    sealed class Option {
        class Indent(val columns: Int) : Option()
    }
}
