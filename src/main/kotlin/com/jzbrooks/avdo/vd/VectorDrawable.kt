package com.jzbrooks.avdo.vd

import com.jzbrooks.avdo.graphic.*

data class VectorDrawable(
        override val elements: List<Element>,
        override val size: Size,
        override val metadata: Map<String, String>,
        override val viewBox: ViewBox = ViewBox(0, 0, size.width.value, size.height.value)
) : Graphic