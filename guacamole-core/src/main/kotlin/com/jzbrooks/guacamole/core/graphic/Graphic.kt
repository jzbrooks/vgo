package com.jzbrooks.guacamole.core.graphic

import com.jzbrooks.guacamole.core.optimization.OptimizationRegistry

interface Graphic : ContainerElement {
    override var elements: List<Element>
    val optimizationRegistry: OptimizationRegistry
}