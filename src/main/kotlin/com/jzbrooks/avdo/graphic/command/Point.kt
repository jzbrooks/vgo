package com.jzbrooks.avdo.graphic.command

data class Point(var x: Float, var y: Float) {
    override fun toString() = "${x.compactString()},${y.compactString()}"
}
