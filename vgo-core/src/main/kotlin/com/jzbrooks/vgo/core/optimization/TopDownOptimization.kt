package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.ElementVisitor

@Deprecated(
    "Has been relocated to the transformation package",
    replaceWith = ReplaceWith("com.jzbrooks.vgo.core.transformation.TopDownTransformation"),
)
interface TopDownOptimization : ElementVisitor
