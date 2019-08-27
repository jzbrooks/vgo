package com.jzbrooks.avdo.graphic

data class Dimension(val value: Int, val unit: Unit = Unit.Px) {
    enum class Unit {
        Px,
        Dp
    }
}