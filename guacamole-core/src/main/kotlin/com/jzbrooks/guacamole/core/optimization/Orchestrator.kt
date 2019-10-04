package com.jzbrooks.guacamole.core.optimization

import com.jzbrooks.guacamole.core.graphic.Graphic

class Orchestrator(private val optimizations: List<Optimization>) {
    fun optimize(graphic: Graphic) = optimizations.forEach { it.optimize(graphic) }
}
