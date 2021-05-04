package com.jzbrooks.vgo.svg.graphic

import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Attributes as CoreAttributes

data class ClipPath(
    override var elements: List<Element>,
    override val attributes: Attributes = Attributes(null, mutableMapOf()),
) : ContainerElement {

    data class Attributes(override val name: String?, override val foreign: MutableMap<String, String>) : CoreAttributes
}
