package com.jzbrooks.avdo.graphic

data class Size(val width: Int, val height: Int) {
    enum class Unit {
        Dp,
        Px,
        Unspecified
    }
}