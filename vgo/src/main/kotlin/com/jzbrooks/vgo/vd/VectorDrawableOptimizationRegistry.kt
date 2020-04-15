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
                SimplifyLineCommands(0.00001f),
                SimplifyBezierCurveCommands(0.00001f),
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