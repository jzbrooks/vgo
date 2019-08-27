package com.jzbrooks.avdo.vd

import com.jzbrooks.avdo.graphic.*

data class VectorDrawable(
        override var paths: List<Path>,
        override val groups: List<Group>,
        override val size: Size,
        override val viewBox: ViewBox = ViewBox(0, 0, size.width.value, size.height.value)
) : Graphic