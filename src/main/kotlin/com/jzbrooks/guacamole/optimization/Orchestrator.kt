package com.jzbrooks.guacamole.optimization

import com.jzbrooks.guacamole.graphic.Graphic

class Orchestrator(private val optimizations: List<Optimization>) {
    fun optimize(graphic: Graphic) = optimizations.forEach { it.visit(graphic) }
}
