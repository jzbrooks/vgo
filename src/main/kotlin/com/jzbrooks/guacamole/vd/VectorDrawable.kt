package com.jzbrooks.guacamole.vd

import com.jzbrooks.guacamole.graphic.Element
import com.jzbrooks.guacamole.graphic.Graphic
import com.jzbrooks.guacamole.graphic.Size
import com.jzbrooks.guacamole.graphic.ViewBox

data class VectorDrawable(
        override var elements: List<Element>,
        override var size: Size,
        override var attributes: Map<String, String>,
        override var viewBox: ViewBox = ViewBox(0, 0, size.width.value, size.height.value)
) : Graphic