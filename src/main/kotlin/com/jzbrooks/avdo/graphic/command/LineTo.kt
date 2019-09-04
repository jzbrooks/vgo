package com.jzbrooks.avdo.graphic.command

data class LineTo(override val arguments: List<Pair<Int, Int>>) : PairArgCommand