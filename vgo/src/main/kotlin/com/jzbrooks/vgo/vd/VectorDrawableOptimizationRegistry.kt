package com.jzbrooks.vgo.vd

import com.jzbrooks.vgo.core.optimization.*
import com.jzbrooks.vgo.vd.optimization.BakeTransformations

class VectorDrawableOptimizationRegistry : OptimizationRegistry(prePass, topDownOptimizations, emptyList(), postPass) {

    companion object {
        private val prePass = listOf(
                BakeTransformations()
        )

        private val topDownOptimizations: List<TopDownOptimization> = listOf(
                BreakoutImplicitCommands(),
                CommandVariant(CommandVariant.Mode.Relative),
                SimplifyLineCommands(1e-3f),
                ConvertCurvesToArcs(VectorDrawableCommandPrinter(3)),
                SimplifyBezierCurveCommands(1e-3f),
                RemoveRedundantCommands(),
                CommandVariant(CommandVariant.Mode.Compact(VectorDrawableCommandPrinter(3)))
        )

        private val postPass = listOf(
                CollapseGroups(),
                MergePaths(),
                RemoveEmptyGroups()
        )
    }
}