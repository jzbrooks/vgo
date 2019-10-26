package com.jzbrooks.guacamole.vd

import com.jzbrooks.guacamole.core.graphic.Graphic
import com.jzbrooks.guacamole.core.optimization.*
import com.jzbrooks.guacamole.core.optimization.OptimizationRegistry
import com.jzbrooks.guacamole.vd.optimization.BakeTransformations

class OptimizationRegistry : OptimizationRegistry {
    override fun apply(graphic: Graphic) {
        for (optimization in optimizations) {
            optimization.optimize(graphic)
        }
    }

    companion object {
        private val optimizations = listOf(
                BakeTransformations(),
                CollapseGroups(),
                MergePaths(),
                BreakoutImplicitCommands(),
                CommandVariant(),
                RemoveEmptyGroups()
        )
    }
}