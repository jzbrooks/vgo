package com.jzbrooks.avdo.graphic.command

data class MoveTo(override val arguments: List<Pair<Int, Int>>) : PairArgCommand