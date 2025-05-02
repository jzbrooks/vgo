package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.transformation.BakeTransformations
import com.jzbrooks.vgo.core.transformation.BreakoutImplicitCommands
import com.jzbrooks.vgo.core.transformation.CollapseGroups
import com.jzbrooks.vgo.core.transformation.CommandVariant
import com.jzbrooks.vgo.core.transformation.ConvertCurvesToArcs
import com.jzbrooks.vgo.core.transformation.MergePaths
import com.jzbrooks.vgo.core.transformation.Polycommands
import com.jzbrooks.vgo.core.transformation.RemoveEmptyGroups
import com.jzbrooks.vgo.core.transformation.RemoveRedundantCommands
import com.jzbrooks.vgo.core.transformation.RemoveTransparentPaths
import com.jzbrooks.vgo.core.transformation.SimplifyBezierCurveCommands
import com.jzbrooks.vgo.core.transformation.SimplifyLineCommands
import com.jzbrooks.vgo.core.transformation.TransformerSet

class SvgOptimizationRegistry :
    TransformerSet(
        bottomUpTransformers =
            listOf(
                BakeTransformations(),
                CollapseGroups(),
                RemoveEmptyGroups(),
                MergePaths(MergePaths.Constraints.None),
            ),
        topDownTransformers =
            listOf(
                RemoveTransparentPaths(),
                BreakoutImplicitCommands(),
                CommandVariant(CommandVariant.Mode.Relative),
                SimplifyLineCommands(1e-3f),
                ConvertCurvesToArcs(ConvertCurvesToArcs.Criterion.ShortestPath(ScalableVectorGraphicCommandPrinter(3))),
                SimplifyBezierCurveCommands(1e-3f),
                RemoveRedundantCommands(),
                CommandVariant(CommandVariant.Mode.Compact(ScalableVectorGraphicCommandPrinter(3))),
                Polycommands(),
            ),
    )
