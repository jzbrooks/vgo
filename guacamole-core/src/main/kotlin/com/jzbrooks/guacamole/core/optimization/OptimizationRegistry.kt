package com.jzbrooks.guacamole.core.optimization

import com.jzbrooks.guacamole.core.graphic.*

abstract class OptimizationRegistry(
        private val topDownOptimizations: List<TopDownOptimization>,
        private val bottomUpOptimization: List<BottomUpOptimization>,
        private val wholeGraphicOptimizations: List<Optimization>
) {

    fun apply(graphic: Graphic) {
        topDownTraversal(graphic)
        bottomUpTraversal(graphic)
        wholeGraphicTraversal(graphic)
    }

    private fun topDownTraversal(element: Element): Element {
        for (optimization in topDownOptimizations) {
            when {
                element is PathElement && optimization is PathElementVisitor -> optimization.visit(element)
                element is Group && optimization is GroupVisitor -> optimization.visit(element)
                element is ContainerElement && optimization is ContainerElementVisitor -> optimization.visit(element)
            }
        }

        return if (element is ContainerElement) element.apply { elements.map(::topDownTraversal) } else element
    }

    private fun bottomUpTraversal(element: Element): Element {
        if (element is ContainerElement) {
            element.apply { elements.map(::topDownTraversal) }
        }

        for (optimization in bottomUpOptimization) {
            when {
                element is PathElement && optimization is PathElementVisitor -> optimization.visit(element)
                element is Group && optimization is GroupVisitor -> optimization.visit(element)
                element is ContainerElement && optimization is ContainerElementVisitor -> optimization.visit(element)
            }
        }

        return element
    }

    private fun wholeGraphicTraversal(graphic: Graphic) {
        for (optimization in wholeGraphicOptimizations) {
            optimization.optimize(graphic)
        }
    }
}
