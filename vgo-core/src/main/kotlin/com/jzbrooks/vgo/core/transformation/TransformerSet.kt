package com.jzbrooks.vgo.core.transformation

import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.util.element.traverseBottomUp
import com.jzbrooks.vgo.core.util.element.traverseTopDown

abstract class TransformerSet(
    private val bottomUpTransformations: List<BottomUpTransformation>,
    private val topDownTransformations: List<TopDownTransformation>,
) {
    fun apply(graphic: Graphic) {
        if (bottomUpTransformations.isNotEmpty()) {
            traverseBottomUp(graphic) { element ->
                for (optimization in bottomUpTransformations) {
                    element.accept(optimization)
                }
            }
        }

        if (topDownTransformations.isNotEmpty()) {
            traverseTopDown(graphic) { element ->
                for (optimization in topDownTransformations) {
                    element.accept(optimization)
                }
            }
        }
    }
}
