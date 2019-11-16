package com.jzbrooks.guacamole.core.optimization

import com.jzbrooks.guacamole.core.graphic.PathElement

interface PathElementVisitor {
    fun visit(pathElement: PathElement)
}