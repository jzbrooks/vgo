package com.jzbrooks.vgo.vd

import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.ElementVisitor
import com.jzbrooks.vgo.core.graphic.Graphic

data class VectorDrawable(
    override var elements: List<Element>,
    override val id: String?,
    override val foreign: MutableMap<String, String>,
) : Graphic {
    override fun accept(visitor: ElementVisitor) = visitor.visit(this)
}
