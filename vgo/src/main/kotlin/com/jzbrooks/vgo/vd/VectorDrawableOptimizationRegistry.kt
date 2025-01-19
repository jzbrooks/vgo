package com.jzbrooks.vgo.vd

import com.jzbrooks.vgo.core.transformation.BakeTransformations
import com.jzbrooks.vgo.core.transformation.BreakoutImplicitCommands
import com.jzbrooks.vgo.core.transformation.CollapseGroups
import com.jzbrooks.vgo.core.transformation.CommandVariant
import com.jzbrooks.vgo.core.transformation.ConvertCurvesToArcs
import com.jzbrooks.vgo.core.transformation.MergePaths
import com.jzbrooks.vgo.core.transformation.Optimizer
import com.jzbrooks.vgo.core.transformation.Polycommands
import com.jzbrooks.vgo.core.transformation.RemoveEmptyGroups
import com.jzbrooks.vgo.core.transformation.RemoveRedundantCommands
import com.jzbrooks.vgo.core.transformation.RemoveTransparentPaths
import com.jzbrooks.vgo.core.transformation.SimplifyBezierCurveCommands
import com.jzbrooks.vgo.core.transformation.SimplifyLineCommands

class VectorDrawableOptimizationRegistry :
    Optimizer(
        bottomUpTransformations =
            listOf(
                BakeTransformations(),
                CollapseGroups(),
                RemoveEmptyGroups(),
                // https://cs.android.com/android/platform/superproject/main/+/2e48e15a8097916063eacc023044bc90bb93c73e:frameworks/base/libs/androidfw/StringPool.cpp;l=328
                MergePaths(MergePaths.Constraints.PathLength(VectorDrawableCommandPrinter(3), (1 shl 15) - 1)),
            ),
        topDownTransformations =
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
