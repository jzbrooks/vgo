package com.jzbrooks.vgo.composable

import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.ElementVisitor
import com.jzbrooks.vgo.core.graphic.Graphic

data class ImageVectorGraphic(
    override var elements: List<Element>,
    override val id: String?,
    val propertyName: String,
    val packageName: String?,
) : Graphic {
    override val foreign: MutableMap<String, String> = mutableMapOf()

    override fun accept(visitor: ElementVisitor) {
        visitor.visit(this)
    }
}
