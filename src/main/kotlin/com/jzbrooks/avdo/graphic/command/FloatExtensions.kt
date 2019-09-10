package com.jzbrooks.avdo.graphic.command

fun Float.compactString(): CharSequence {
    val compactvalue: Number = if (this.rem(1f) == 0f) this.toInt() else this
    return compactvalue.toString()
}