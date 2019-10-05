package com.jzbrooks.guacamole.vd

import com.jzbrooks.guacamole.core.optimization.*

class VectorDrawableOptimizationRegistry : OptimizationRegistry {
    override val optimizations = listOf(
            BakeTransformations(vectorDrawableTransformationPropertyNames),
            CollapseGroups(),
            MergePaths(),
            CommandVariant(),
            RemoveEmptyGroups()
    )

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