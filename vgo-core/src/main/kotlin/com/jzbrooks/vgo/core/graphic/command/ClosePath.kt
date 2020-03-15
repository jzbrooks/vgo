package com.jzbrooks.vgo.core.graphic.command

class ClosePath : Command {
    override fun equals(other: Any?) = other is ClosePath
    override fun hashCode() = javaClass.hashCode()
}