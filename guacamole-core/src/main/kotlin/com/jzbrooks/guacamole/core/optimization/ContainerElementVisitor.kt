package com.jzbrooks.guacamole.core.optimization

import com.jzbrooks.guacamole.core.graphic.ContainerElement

interface ContainerElementVisitor {
    fun visit(containerElement: ContainerElement)
}