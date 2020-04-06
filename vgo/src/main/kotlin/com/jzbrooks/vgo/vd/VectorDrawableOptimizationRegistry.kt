package com.jzbrooks.vgo.vd

import com.jzbrooks.vgo.core.optimization.*
import com.jzbrooks.vgo.vd.optimization.BakeTransformations

class VectorDrawableOptimizationRegistry : OptimizationRegistry(topDownOptimizations, emptyList(), wholeGraphicOptimizations) {

    companion object {
        private val topDownOptimizations: List<TopDownOptimization> = listOf(
                BakeTransformations(),
                BreakoutImplicitCommands(),
                CommandVariant(CommandVariant.Mode.Relative),
                SimplifyLineCommands(0.00001f),
                SimplifyBezierCurveCommands(0.00001f),
                RemoveRedundantCommands(),
                CommandVariant(CommandVariant.Mode.Compact(VectorDrawableCommandPrinter(3)))
        )

        private val wholeGraphicOptimizations = listOf(
                CollapseGroups(),
                MergePaths(),
                RemoveEmptyGroups()
        )
    }
}