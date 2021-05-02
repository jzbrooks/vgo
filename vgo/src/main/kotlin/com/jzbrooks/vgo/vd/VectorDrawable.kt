package com.jzbrooks.vgo.vd

import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Graphic

data class VectorDrawable(
    override var elements: List<Element>,
    override var attributes: MutableMap<String, String>
) : Graphic
