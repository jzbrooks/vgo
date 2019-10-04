package com.jzbrooks.guacamole.core.util.math

fun Float.compactString(): CharSequence {
    val compactValue: Number = if (this.rem(1f) == 0f) this.toInt() else this
    return compactValue.toString()
}