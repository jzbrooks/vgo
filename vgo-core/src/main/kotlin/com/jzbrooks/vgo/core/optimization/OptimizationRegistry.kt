package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.PathElement
import com.jzbrooks.vgo.core.util.element.traverseBottomUp
import com.jzbrooks.vgo.core.util.element.traverseTopDown

abstract class OptimizationRegistry(
    private val bottomUpOptimizations: List<BottomUpOptimization>,
    private val topDownOptimizations: List<TopDownOptimization>,
    private val wholeGraphic: List<Optimization>
) {

    fun apply(graphic: Graphic) {
        if (bottomUpOptimizations.isNotEmpty()) {
            traverseBottomUp(graphic) { element ->
                for (optimization in bottomUpOptimizations) {
                    when {
                        element is PathElement && optimization is PathElementVisitor -> optimization.visit(element)
                        element is Group && optimization is GroupVisitor -> optimization.visit(element)
                        element is ContainerElement && optimization is ContainerElementVisitor -> optimization.visit(element)
                    }
                }
            }
        }

        if (topDownOptimizations.isNotEmpty()) {
            traverseTopDown(graphic) { element ->
                for (optimization in topDownOptimizations) {
                    when {
                        element is PathElement && optimization is PathElementVisitor -> optimization.visit(element)
                        element is Group && optimization is GroupVisitor -> optimization.visit(element)
                        element is ContainerElement && optimization is ContainerElementVisitor -> optimization.visit(element)
                    }
                }
            }
        }

        for (optimization in wholeGraphic) {
            optimization.optimize(graphic)
        }
    }
}
