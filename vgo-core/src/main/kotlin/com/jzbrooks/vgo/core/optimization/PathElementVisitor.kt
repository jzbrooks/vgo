package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.PathElement

interface PathElementVisitor {
    fun visit(pathElement: PathElement)
}