package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Attributes as CoreAttributes

data class ScalableVectorGraphic(
    override var elements: List<Element>,
    override var attributes: Attributes = Attributes(null, mutableMapOf()),
) : Graphic {

    data class Attributes(override val id: String?, override val foreign: MutableMap<String, String>) : CoreAttributes
}
