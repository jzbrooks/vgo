package com.jzbrooks.vgo.core.transformation

import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.util.element.traverseBottomUp
import com.jzbrooks.vgo.core.util.element.traverseTopDown

abstract class TransformerSet(
    private val bottomUpTransformers: List<BottomUpTransformer>,
    private val topDownTransformers: List<TopDownTransformer>,
) {
    fun apply(graphic: Graphic) {
        if (bottomUpTransformers.isNotEmpty()) {
            traverseBottomUp(graphic) { element ->
                for (optimization in bottomUpTransformers) {
                    element.accept(optimization)
                }
            }
        }

        if (topDownTransformers.isNotEmpty()) {
            traverseTopDown(graphic) { element ->
                for (optimization in topDownTransformers) {
                    element.accept(optimization)
                }
            }
        }
    }
}
