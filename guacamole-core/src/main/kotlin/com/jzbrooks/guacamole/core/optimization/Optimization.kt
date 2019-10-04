package com.jzbrooks.guacamole.core.optimization

import com.jzbrooks.guacamole.core.graphic.Graphic

interface Optimization {
    fun optimize(graphic: Graphic)
}