package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.graphic.Attributes as CoreAttributes

/**
 * Represents an image element that is not otherwise
 * explicitly represented by the core api
 */
data class Extra(
    var name: String,
    override var elements: List<Element>,
    override var attributes: Attributes = Attributes(null, mutableMapOf()),
) : ContainerElement {

    data class Attributes(override val id: String?, override val foreign: MutableMap<String, String>) : CoreAttributes
}
