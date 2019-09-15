package com.jzbrooks.avdo.optimization

import com.jzbrooks.avdo.graphic.Element

interface Optimization<T : Element> {
    fun visit(element: T)
}