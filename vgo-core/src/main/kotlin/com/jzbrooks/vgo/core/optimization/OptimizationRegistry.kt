package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.util.element.traverseBottomUp
import com.jzbrooks.vgo.core.util.element.traverseTopDown

@Suppress("DEPRECATION")
@Deprecated("Has been relocated to the transformation package", replaceWith = ReplaceWith("com.jzbrooks.vgo.core.transformation.Optimizer"))
abstract class OptimizationRegistry(
    private val bottomUpOptimizations: List<BottomUpOptimization>,
    private val topDownOptimizations: List<TopDownOptimization>,
) {
    fun apply(graphic: Graphic) {
        if (bottomUpOptimizations.isNotEmpty()) {
            traverseBottomUp(graphic) { element ->
                for (optimization in bottomUpOptimizations) {
                    element.accept(optimization)
                }
            }
        }

        if (topDownOptimizations.isNotEmpty()) {
            traverseTopDown(graphic) { element ->
                for (optimization in topDownOptimizations) {
                    element.accept(optimization)
                }
            }
        }
    }
}
