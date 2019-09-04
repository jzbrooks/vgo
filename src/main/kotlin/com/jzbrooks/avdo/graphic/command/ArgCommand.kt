package com.jzbrooks.avdo.graphic.command

interface ArgCommand : Command {
    val arguments: List<Int>
}