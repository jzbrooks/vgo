package com.jzbrooks.vgo.vd

import com.jzbrooks.vgo.core.optimization.BreakoutImplicitCommands
import com.jzbrooks.vgo.core.optimization.CollapseGroups
import com.jzbrooks.vgo.core.optimization.CommandVariant
import com.jzbrooks.vgo.core.optimization.ConvertCurvesToArcs
import com.jzbrooks.vgo.core.optimization.MergePaths
import com.jzbrooks.vgo.core.optimization.OptimizationRegistry
import com.jzbrooks.vgo.core.optimization.Polycommands
import com.jzbrooks.vgo.core.optimization.RemoveEmptyGroups
import com.jzbrooks.vgo.core.optimization.RemoveRedundantCommands
import com.jzbrooks.vgo.core.optimization.SimplifyBezierCurveCommands
import com.jzbrooks.vgo.core.optimization.SimplifyLineCommands
import com.jzbrooks.vgo.core.optimization.TopDownOptimization
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
            CommandVariant(CommandVariant.Mode.Compact(VectorDrawableCommandPrinter(3))),
            Polycommands()
        )

        private val postPass = listOf(
            CollapseGroups(),
            RemoveEmptyGroups(),
            MergePaths()
        )
    }
}
