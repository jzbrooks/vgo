package com.jzbrooks.avdo.vd

import com.jzbrooks.avdo.*

data class VectorDrawable(
        override var paths: List<Path>,
        override val groups: List<Group>,
        override val size: Size,
        override val viewbox: Viewbox = Viewbox(0, 0, size.width, size.height)
) : Graphic