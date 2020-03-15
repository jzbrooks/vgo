package com.jzbrooks.vgo

class ArgReader(private val args: MutableList<String>) {

    private val hasArguments
        get() = args.isNotEmpty()

    fun readFlag(name: String): Boolean {
        require(name.isNotBlank())

        val names = name.split('|')
        if (names.size > 1)
            return names.any(::readFlag)

        val index = args.indexOfFirst { isOptionArgument(name, it) }
        if (index == -1) return false

        args.removeAt(index)
        return true
    }

    fun readOption(name: String): String? {
        require(name.isNotBlank())

        val names = name.split('|')
        if (names.size > 1)
            return names.map(::readOption).firstOrNull { it != null }

        val index = args.indexOfFirst { isOptionArgument(name, it) }
        if (index == -1) return null

        val value = args.getOrElse(index + 1) {
            throw IllegalStateException("Missing value after ${if (name.length == 1) "-" else "--"}$name")
        }

        args.removeAt(index)
        args.removeAt(index)
        return value
    }

    fun readArguments(): List<String> {
        val arguments = mutableListOf<String>()
        while (hasArguments) {
            arguments.add(readArgument())
        }
        return arguments
    }

    private fun readArgument(): String {
        val value = args.first()

        check(!isOption(value)) { "Unexpected option $value" }

        args.removeAt(0)
        return value
    }

    companion object {
        private fun isOption(name: String) = name.length >= 2 && name[0] == '-'
        private fun isOptionArgument(name: String, argument: String): Boolean {
            return if (name.length == 1) {
                "-$name" == argument
            } else {
                "--$name" == argument
            }
        }
    }
}