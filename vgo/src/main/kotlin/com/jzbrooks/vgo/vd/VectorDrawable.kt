package com.jzbrooks.vgo.vd

import com.jzbrooks.vgo.core.graphic.Attributes as CoreAttributes
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Graphic

data class VectorDrawable(
    override var elements: List<Element>,
    override var attributes: Attributes = Attributes(null, mutableMapOf()),
) : Graphic {

    data class Attributes(override val name: String?, override val foreign: MutableMap<String, String>) : CoreAttributes

}