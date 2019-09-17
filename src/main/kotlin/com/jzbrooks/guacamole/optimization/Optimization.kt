package com.jzbrooks.guacamole.optimization

import com.jzbrooks.guacamole.graphic.Element

interface Optimization<T : Element> {
    fun visit(element: T)
}