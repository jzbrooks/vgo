package com.jzbrooks.vgo.core.graphic

import dev.romainguy.kotlin.math.Mat3

data class Group(
    override var elements: List<Element>,
    override val id: String? = null,
    override val foreign: MutableMap<String, String> = mutableMapOf(),
    var transform: Mat3 = Mat3.identity()
) : ContainerElement {
    override fun accept(visitor: ElementVisitor) = visitor.visit(this)
}
