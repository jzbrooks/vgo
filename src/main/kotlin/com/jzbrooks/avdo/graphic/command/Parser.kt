package com.jzbrooks.avdo.graphic.command

import java.lang.IllegalStateException

inline class CommandString(val data: String) {
    fun toCommandList(): List<Command> {
        return data.split(commandRegex)
                .asSequence()
                .filter { it.isNotBlank() }
                .map { command ->
                    val upperCommand = command.toUpperCase()
                    val variant = if (command[0].isLowerCase()) VariantCommand.Variant.RELATIVE else VariantCommand.Variant.ABSOLUTE
                    when {
                        upperCommand.startsWith("M") -> {
                            val data = argumentPairs.findAll(command)
                                    .map { match ->
                                        val components = match.value.split(Regex("[,\\s]"))
                                        components[0].toInt() to components[1].toInt()
                                    }
                                    .toList()
                            MoveTo(variant, data)
                        }
                        upperCommand.startsWith("L") -> {
                            val data = argumentPairs.findAll(command)
                                    .map { match ->
                                        val components = match.value.split(Regex("[,\\s]"))
                                        components[0].toInt() to components[1].toInt()
                                    }
                                    .toList()

                            LineTo(variant, data)
                        }
                        upperCommand.startsWith("V") -> {
                            val data = arguments.findAll(command)
                                    .map { it.value.toInt() }
                                    .toList()

                            VerticalLineTo(variant, data)
                        }
                        upperCommand.startsWith("H") -> {
                            val data = arguments.findAll(command)
                                    .map { it.value.toInt() }
                                    .toList()

                            HorizontalLineTo(variant, data)
                        }
                        command.startsWith("Z") -> ClosePath()
                        else -> throw IllegalStateException("Expected one of $commandRegex but was $command")
                    }
                }.toList()
    }

    companion object {
        private val commandRegex = Regex("(?=[MmLlHhVvCcSsQqTtAaZz])\\s*")
        private val number = Regex("[-+]?(?:\\d*\\.\\d+|\\d+\\.?)([eE][-+]?\\d+)?")
        private val arguments = Regex("$number")
        private val argumentPairs = Regex("$number[,\\s]$number")
    }
}
