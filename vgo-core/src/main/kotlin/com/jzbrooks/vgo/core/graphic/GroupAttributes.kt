package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.util.math.Matrix3

interface GroupAttributes : Attributes {
    var transform: Matrix3

    fun isEmpty(): Boolean
}
