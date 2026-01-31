package com.jzbrooks.vgo.core

import com.jzbrooks.vgo.core.graphic.Graphic
import java.io.OutputStream

@Deprecated("Use format-specific writers to avoid creating multiple copies of the graphic representation")
interface Writer<in T : Graphic> {
    val options: Set<Option>

    fun write(
        graphic: T,
        stream: OutputStream,
    )

    sealed class Option {
        class Indent(
            val columns: Int,
        ) : Option()
    }
}
