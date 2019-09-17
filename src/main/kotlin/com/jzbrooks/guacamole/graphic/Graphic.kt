package com.jzbrooks.guacamole.graphic

interface Graphic : Element {
    var elements: List<Element>
    var size: Size
    var viewBox: ViewBox
}