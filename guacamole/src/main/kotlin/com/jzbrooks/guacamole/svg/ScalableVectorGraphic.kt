package com.jzbrooks.guacamole.svg

import com.jzbrooks.guacamole.core.graphic.Element
import com.jzbrooks.guacamole.core.graphic.Graphic

data class ScalableVectorGraphic(
    override var elements: List<Element>,
    override var attributes: MutableMap<String, String>
) : Graphic