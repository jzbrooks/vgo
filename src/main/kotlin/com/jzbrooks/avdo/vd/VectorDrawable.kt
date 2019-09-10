package com.jzbrooks.avdo.vd

import com.jzbrooks.avdo.graphic.Element
import com.jzbrooks.avdo.graphic.Graphic
import com.jzbrooks.avdo.graphic.Size
import com.jzbrooks.avdo.graphic.ViewBox

data class VectorDrawable(
        override var elements: List<Element>,
        override var size: Size,
        override var metadata: Map<String, String>,
        override var viewBox: ViewBox = ViewBox(0, 0, size.width.value, size.height.value)
) : Graphic