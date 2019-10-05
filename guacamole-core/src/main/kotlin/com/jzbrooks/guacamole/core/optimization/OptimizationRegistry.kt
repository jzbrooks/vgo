package com.jzbrooks.guacamole.core.optimization

import com.jzbrooks.guacamole.core.graphic.Graphic

interface OptimizationRegistry {
    fun apply(graphic: Graphic)
}
