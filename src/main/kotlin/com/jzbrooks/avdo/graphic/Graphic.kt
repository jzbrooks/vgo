package com.jzbrooks.avdo.graphic

interface Graphic : Element {
    val elements: List<Element>
    val size: Size
    val viewBox: ViewBox
}