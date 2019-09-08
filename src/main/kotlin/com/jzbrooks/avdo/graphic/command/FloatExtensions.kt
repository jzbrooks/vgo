package com.jzbrooks.avdo.graphic.command

fun Float.compactString(): CharSequence {
    val compactValue: Number = if (this.rem(1f) == 0f) this.toInt() else this
    return compactValue.toString()
}