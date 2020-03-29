package com.jzbrooks.vgo.vd.optimization

import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.PathElement
import com.jzbrooks.vgo.core.optimization.ContainerElementVisitor
import com.jzbrooks.vgo.core.optimization.PathElementVisitor
import com.jzbrooks.vgo.core.optimization.TopDownOptimization
import com.jzbrooks.vgo.vd.VectorDrawable

class RemoveRedundantAttributes : TopDownOptimization, ContainerElementVisitor, PathElementVisitor {
    override fun visit(containerElement: ContainerElement) {
        if (containerElement is VectorDrawable) {
            val attributes = containerElement.attributes

            attributes.remove("android:alpha", "1.0")
            attributes.remove("android:autoMirrored", "false")
            attributes.remove("android:tintMode", "src_in")
        }
    }

    override fun visit(pathElement: PathElement) {
        val attributes = pathElement.attributes

        attributes.remove("android:strokeWidth", "0")
        attributes.remove("android:strokeAlpha", "1")
        attributes.remove("android:strokeLineCap", "butt")
        attributes.remove("android:strokeLineJoin", "miter")
        attributes.remove("android:strokeMiterLimit", "4")
        attributes.remove("android:fillAlpha", "1")
        attributes.remove("android:fillType", "nonZero")
        attributes.remove("android:trimPathStart", "0")
        attributes.remove("android:trimPathEnd", "1")
        attributes.remove("android:trimPathOffset", "0")
    }
}