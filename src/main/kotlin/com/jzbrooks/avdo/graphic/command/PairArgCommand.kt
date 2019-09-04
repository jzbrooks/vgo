package com.jzbrooks.avdo.graphic.command

interface PairArgCommand : Command {
    val arguments: List<Pair<Int, Int>>
}