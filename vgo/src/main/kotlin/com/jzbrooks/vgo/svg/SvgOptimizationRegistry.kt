package com.jzbrooks.vgo.core.svg

import com.jzbrooks.vgo.core.optimization.*
import com.jzbrooks.vgo.core.svg.optimization.BakeTransformations

class SvgOptimizationRegistry : OptimizationRegistry(emptyList(), topDownOptimizations, emptyList(), wholeGraphicOptimizations) {

    companion object {
        private val topDownOptimizations: List<TopDownOptimization> = listOf(
                BakeTransformations(),
                BreakoutImplicitCommands(),
                CommandVariant(CommandVariant.Mode.Relative),
                SimplifyLineCommands(1e-3f),
                ConvertCurvesToArcs(ScalableVectorGraphicCommandPrinter(3)),
                SimplifyBezierCurveCommands(1e-3f),
                RemoveRedundantCommands(),
                CommandVariant(CommandVariant.Mode.Compact(ScalableVectorGraphicCommandPrinter(3))),
                Polycommands()
        )

        private val wholeGraphicOptimizations = listOf(
                CollapseGroups(),
                RemoveEmptyGroups(),
                MergePaths()
        )
    }
}