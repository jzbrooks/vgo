package com.jzbrooks.guacamole.vd

import com.jzbrooks.guacamole.graphic.Element
import com.jzbrooks.guacamole.graphic.Graphic

data class VectorDrawable(
        override var elements: List<Element>,
        override var attributes: Map<String, String>
) : Graphic