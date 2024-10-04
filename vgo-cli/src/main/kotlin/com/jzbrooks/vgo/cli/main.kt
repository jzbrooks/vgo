package com.jzbrooks.vgo.cli

import com.jzbrooks.vgo.Application
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val argReader = ArgReader(args.toMutableList())

    val printHelp = argReader.readFlag("help|h")
    val printVersion = argReader.readFlag("version|v")
    val printStats = argReader.readFlag("stats|s")
    val indent = argReader.readOption("indent")?.toIntOrNull()

    val outputs = run {
        val outputPaths = mutableListOf<String>()
        var output = argReader.readOption("output|o")
        while (output != null) {
            outputPaths.add(output)
            output = argReader.readOption("output|o")
        }
        outputPaths.toList()
    }

    var format = argReader.readOption("format")

    var inputs = argReader.readArguments()

    val options = Application.Options(
        printHelp = printHelp,
        printVersion = printVersion,
        printStats = printStats,
        indent = indent,
        output = outputs,
        format = format,
        input = inputs
    )

    exitProcess(Application(options).run())
}