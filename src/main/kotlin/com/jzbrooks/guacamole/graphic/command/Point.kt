package com.jzbrooks.guacamole.graphic.command

data class Point(var x: Float, var y: Float) {
    override fun toString() = "${x.compactString()},${y.compactString()}"

    operator fun plus(other: Point): Point {
        return Point(x + other.x, y + other.y)
    }

    operator fun minus(other: Point): Point {
        return Point(x - other.x, y - other.y)
    }
}
