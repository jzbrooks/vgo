package com.jzbrooks.guacamole.util.math

data class Vector3(val i: Float, val j: Float, val k: Float) {
    constructor(point: Point) : this(point.x, point.y, 1f)
    fun toPoint() = Point(i, j)
}