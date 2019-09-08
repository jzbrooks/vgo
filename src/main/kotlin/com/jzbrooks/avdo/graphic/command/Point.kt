package com.jzbrooks.avdo.graphic.command

data class Point(val x: Float, val y: Float) {
    override fun toString() = "${x.compactString()},${y.compactString()}"
}
