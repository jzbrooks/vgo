package com.jzbrooks.avdo.graphic.command

import java.lang.IllegalStateException

inline class CommandString(val data: String) {
    fun toCommandList(): List<Command> {
        return data.split(commandRegex)
                .asSequence()
                .filter { it.isNotBlank() }
                .map { command ->
                    val upperCommand = command.toUpperCase()
                    val variant = if (command[0].isLowerCase()) CommandVariant.RELATIVE else CommandVariant.ABSOLUTE
                    when {
                        upperCommand.startsWith("M") -> {
                            val data = argumentPairs.findAll(command)
                                    .map(::mapArgumentPairs)
                                    .toList()
                            MoveTo(variant, data)
                        }
                        upperCommand.startsWith("L") -> {
                            val data = argumentPairs.findAll(command)
                                    .map(::mapArgumentPairs)
                                    .toList()

                            LineTo(variant, data)
                        }
                        upperCommand.startsWith("V") -> {
                            val data = arguments.findAll(command)
                                    .map { it.value.toFloat() }
                                    .toList()

                            VerticalLineTo(variant, data)
                        }
                        upperCommand.startsWith("H") -> {
                            val data = arguments.findAll(command)
                                    .map { it.value.toFloat() }
                                    .toList()

                            HorizontalLineTo(variant, data)
                        }
                        command.startsWith("Z") -> ClosePath()
                        else -> throw IllegalStateException("Expected one of $commandRegex but was $command")
                    }
                }.toList()
    }

    private fun mapArgumentPairs(match: MatchResult): Point<Float> {
        val components = match.value.split(Regex("[,\\s]"))
        return Point(components[0].toFloat(), components[1].toFloat())
    }

    companion object {
        private val commandRegex = Regex("(?=[MmLlHhVvCcSsQqTtAaZz])\\s*")
        private val number = Regex("[-+]?(?:\\d*\\.\\d+|\\d+\\.?)([eE][-+]?\\d+)?")
        private val arguments = Regex("$number")
        private val argumentPairs = Regex("$number[,\\s]$number")
    }
}
