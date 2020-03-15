package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.ContainerElement

interface ContainerElementVisitor {
    fun visit(containerElement: ContainerElement)
}