package com.jzbrooks.vgo.core.graphic.command

interface CommandPrinter {
    fun print(command: Command): String
}