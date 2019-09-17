package com.jzbrooks.guacamole.graphic

data class Dimension(var value: Int, var unit: Unit = Unit.Px) {
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