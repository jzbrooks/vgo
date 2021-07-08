package com.jzbrooks.vgo.core.graphic

/**
 * Represents an image element that is not otherwise
 * explicitly represented by the core api
 */
data class Extra(
    var name: String,
    override var elements: List<Element>,
    override val id: String?,
    override val foreign: MutableMap<String, String>,
) : ContainerElement {
    override fun accept(visitor: ElementVisitor) = visitor.visit(this)
}
