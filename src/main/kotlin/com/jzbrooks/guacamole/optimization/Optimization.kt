package com.jzbrooks.guacamole.optimization

import com.jzbrooks.guacamole.graphic.Graphic

interface Optimization {
    fun optimize(graphic: Graphic)
}