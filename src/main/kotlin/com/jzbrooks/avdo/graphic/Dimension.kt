package com.jzbrooks.avdo.graphic

data class Dimension(val value: Int, val unit: Unit = Unit.Px) {
    override fun toString(): String {
        return value.toString() + unit.toString()
    }
    enum class Unit {
        Px {
            override fun toString() = ""
        },
        Dp {
            override fun toString() = "dp"
        }
    }
}