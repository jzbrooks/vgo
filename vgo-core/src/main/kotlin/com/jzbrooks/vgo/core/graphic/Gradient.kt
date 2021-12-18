package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.Color

sealed interface Gradient : Element {
    val startColor: Color
    val centerColor: Color
    val endColor: Color
    val tileMode: TileMode

    data class Linear(
        override val id: String?,
        override val foreign: MutableMap<String, String>,
        override val startColor: Color,
        override val centerColor: Color,
        override val endColor: Color,
        override val tileMode: TileMode,
    ) : Gradient {
        override fun accept(visitor: ElementVisitor) = visitor.visit(this)
    }

    data class Radial(
        override val id: String?,
        override val foreign: MutableMap<String, String>,
        override val startColor: Color,
        override val centerColor: Color,
        override val endColor: Color,
        override val tileMode: TileMode,
    ) : Gradient {
        override fun accept(visitor: ElementVisitor) = visitor.visit(this)
    }

    enum class TileMode {
        CLAMP,
        REPEAT,
        MIRROR,
    }
}