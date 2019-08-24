package com.jzbrooks.avdo.vd

import com.jzbrooks.avdo.Graphic
import com.jzbrooks.avdo.Group
import com.jzbrooks.avdo.Path

data class VectorDrawable(
        override var paths: List<Path>,
        override val groups: List<Group>,
        override val width: Int,
        override val height: Int
) : Graphic