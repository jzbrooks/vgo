package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.optimization.BakeTransformations
import com.jzbrooks.vgo.core.optimization.BreakoutImplicitCommands
import com.jzbrooks.vgo.core.optimization.CollapseGroups
import com.jzbrooks.vgo.core.optimization.CommandVariant
import com.jzbrooks.vgo.core.optimization.ConvertCurvesToArcs
import com.jzbrooks.vgo.core.optimization.MergePaths
import com.jzbrooks.vgo.core.optimization.OptimizationRegistry
import com.jzbrooks.vgo.core.optimization.Polycommands
import com.jzbrooks.vgo.core.optimization.RemoveEmptyGroups
import com.jzbrooks.vgo.core.optimization.RemoveRedundantCommands
import com.jzbrooks.vgo.core.optimization.RemoveTransparentPaths
import com.jzbrooks.vgo.core.optimization.SimplifyBezierCurveCommands
import com.jzbrooks.vgo.core.optimization.SimplifyLineCommands

class SvgOptimizationRegistry :
    OptimizationRegistry(
        bottomUpOptimizations =
            listOf(
                BakeTransformations(),
                CollapseGroups(),
                RemoveEmptyGroups(),
                MergePaths(MergePaths.Constraints.None),
            ),
        topDownOptimizations =
            listOf(
                RemoveTransparentPaths(),
                BreakoutImplicitCommands(),
                CommandVariant(CommandVariant.Mode.Relative),
                SimplifyLineCommands(1e-3f),
                ConvertCurvesToArcs(ScalableVectorGraphicCommandPrinter(3)),
                SimplifyBezierCurveCommands(1e-3f),
                RemoveRedundantCommands(),
                CommandVariant(CommandVariant.Mode.Compact(ScalableVectorGraphicCommandPrinter(3))),
                Polycommands(),
            ),
    )
