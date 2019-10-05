package com.jzbrooks.guacamole.vd

import com.jzbrooks.guacamole.core.graphic.Graphic
import com.jzbrooks.guacamole.core.optimization.*

class VectorDrawableOptimizationRegistry : OptimizationRegistry {
    private val optimizations = listOf(
            BakeTransformations(vectorDrawableTransformationPropertyNames),
            CollapseGroups(),
            MergePaths(),
            CommandVariant(),
            RemoveEmptyGroups()
    )

    override fun apply(graphic: Graphic) {
        for (optimization in optimizations) {
            optimization.optimize(graphic)
        }
    }

    companion object {
        private val vectorDrawableTransformationPropertyNames = setOf(
                "android:scaleX",
                "android:scaleY",
                "android:translateX",
                "android:translateY",
                "android:pivotX",
                "android:pivotY",
                "android:rotation"
        )
    }
}