package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.optimization.*
import com.jzbrooks.vgo.svg.optimization.BakeTransformations

class SvgOptimizationRegistry : OptimizationRegistry(emptyList(), topDownOptimizations, emptyList(), wholeGraphicOptimizations) {

    companion object {
        private val topDownOptimizations: List<TopDownOptimization> = listOf(
                BakeTransformations(),
                BreakoutImplicitCommands(),
                CommandVariant(CommandVariant.Mode.Relative),
                SimplifyLineCommands(0.00001f),
                SimplifyBezierCurveCommands(0.00001f),
                RemoveRedundantCommands(),
                CommandVariant(CommandVariant.Mode.Compact(ScalableVectorGraphicCommandPrinter(3)))
        )

        private val wholeGraphicOptimizations = listOf(
                CollapseGroups(),
                MergePaths(),
                RemoveEmptyGroups()
        )
    }
}