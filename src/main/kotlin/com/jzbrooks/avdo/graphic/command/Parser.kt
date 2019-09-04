package com.jzbrooks.avdo.graphic.command

import java.lang.IllegalStateException

inline class CommandString(val data: String) {
    fun toCommandList(): List<Command> {
        return data.split(commandRegex)
                .asSequence()
                .filter { it.isNotBlank() }
                .map { command ->
                    when {
                        command.startsWith("M") -> {
                            val data = pathData.findAll(command)
                                    .map { it.value.toInt() }
                                    .zipWithNext { a, b -> Pair(a, b) }
                                    .toList()
                            MoveTo(data)
                        }
                        command.startsWith("L") -> {
                            val data = pathData.findAll(command)
                                    .map { it.value.toInt() }
                                    .zipWithNext { a, b -> Pair(a, b) }
                                    .toList()

                            LineTo(data)
                        }
                        command.startsWith("V") -> {
                            val data = pathData.findAll(command)
                                    .map { it.value.toInt() }
                                    .toList()

                            VerticalLineTo(data)
                        }
                        command.startsWith("H") -> {
                            val data = pathData.findAll(command)
                                    .map { it.value.toInt() }
                                    .toList()

                            HorizontalLineTo(data)
                        }
                        command.startsWith("Z") -> ClosePath()
                        else -> throw IllegalStateException("Expected one of $commandRegex but was $command")
                    }
                }.toList()
    }

    companion object {
        val commandRegex = Regex("(?=[MmLlHhVvCcSsQqTtAaZz])\\s*")
        val pathData = Regex("[-+]?(?:\\d*\\.\\d+|\\d+\\.?)([eE][-+]?\\d+)?")
    }
}
