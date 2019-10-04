package com.jzbrooks.guacamole.graphic

interface Graphic : ContainerElement {
    override var elements: List<Element>
}