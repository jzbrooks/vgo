package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.PathElement

abstract class OptimizationRegistry(
    private val bottomUpOptimizations: List<BottomUpOptimization>,
    private val topDownOptimizations: List<TopDownOptimization>,
    private val wholeGraphic: List<Optimization>
) {

    fun apply(graphic: Graphic) {
        if (bottomUpOptimizations.isNotEmpty()) {
            graphic.elements = graphic.elements.map(::bottomUpTraversal)
        }

        if (topDownOptimizations.isNotEmpty()) {
            graphic.elements = graphic.elements.map(::topDownTraversal)
        }

        for (optimization in wholeGraphic) {
            optimization.optimize(graphic)
        }
    }

    private fun topDownTraversal(element: Element): Element {
        for (optimization in topDownOptimizations) {
            when {
                element is PathElement && optimization is PathElementVisitor -> optimization.visit(element)
                element is Group && optimization is GroupVisitor -> optimization.visit(element)
                element is ContainerElement && optimization is ContainerElementVisitor -> optimization.visit(element)
            }
        }

        return if (element is ContainerElement) element.apply { elements = elements.map(::topDownTraversal) } else element
    }

    private fun bottomUpTraversal(element: Element): Element {
        if (element is ContainerElement) {
            element.apply { elements = elements.map(::bottomUpTraversal) }
        }

        for (optimization in bottomUpOptimizations) {
            when {
                element is PathElement && optimization is PathElementVisitor -> optimization.visit(element)
                element is Group && optimization is GroupVisitor -> optimization.visit(element)
                element is ContainerElement && optimization is ContainerElementVisitor -> optimization.visit(element)
            }
        }

        return element
    }
}
