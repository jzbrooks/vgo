package com.jzbrooks.vgo.vd

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

class VectorDrawableOptimizationRegistry :
    OptimizationRegistry(
        bottomUpOptimizations =
            listOf(
                BakeTransformations(),
                CollapseGroups(),
                RemoveEmptyGroups(),
                // https://cs.android.com/android/platform/superproject/main/+/2e48e15a8097916063eacc023044bc90bb93c73e:frameworks/base/libs/androidfw/StringPool.cpp;l=328
                MergePaths(MergePaths.Constraints.PathLength(VectorDrawableCommandPrinter(3), (1 shl 15) - 1)),
            ),
        topDownOptimizations =
            listOf(
                RemoveTransparentPaths(),
                BreakoutImplicitCommands(),
                CommandVariant(CommandVariant.Mode.Relative),
                SimplifyLineCommands(1e-3f),
                ConvertCurvesToArcs(VectorDrawableCommandPrinter(3)),
                SimplifyBezierCurveCommands(1e-3f),
                RemoveRedundantCommands(),
                CommandVariant(CommandVariant.Mode.Compact(VectorDrawableCommandPrinter(3))),
                Polycommands(),
            ),
    )
