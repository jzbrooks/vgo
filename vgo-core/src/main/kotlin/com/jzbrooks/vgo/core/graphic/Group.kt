package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.graphic.Attributes as CoreAttributes

data class Group(
    override var elements: List<Element>,
    override val attributes: Attributes = Attributes(null, mutableMapOf()), // todo(maybe): remove the default parameter?
) : ContainerElement {

    data class Attributes(override val name: String?, override val foreign: MutableMap<String, String>) : CoreAttributes
}
