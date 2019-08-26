package com.jzbrooks.avdo.graphic

interface Graphic {
    val paths: List<Path>
    val groups: List<Group>
    val size: Size
    val viewBox: ViewBox
}