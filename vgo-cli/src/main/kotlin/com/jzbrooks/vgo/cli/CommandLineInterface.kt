package com.jzbrooks.vgo.cli

import com.jzbrooks.vgo.Vgo
import kotlin.system.exitProcess

class CommandLineInterface {
    fun run(args: Array<String>): Int {
        val argReader = ArgReader(args.toMutableList())

        val printHelp = argReader.readFlag("help|h")
        if (printHelp) {
            println(HELP_MESSAGE)
            return 0
        }

        val printVersion = argReader.readFlag("version|v")
        val printStats = argReader.readFlag("stats|s")
        val indent = argReader.readOption("indent")?.toIntOrNull()

        val outputs =
            run {
                val outputPaths = mutableListOf<String>()
                var output = argReader.readOption("output|o")
                while (output != null) {
                    outputPaths.add(output)
                    output = argReader.readOption("output|o")
                }
                outputPaths.toList()
            }

        val format = argReader.readOption("format")
        val noOptimization = argReader.readFlag("no-optimization")

        if (format == null && noOptimization) {
            System.err.println("Warning: skipping optimization without --format is a no-op.")
        }

        val inputs = argReader.readArguments()

        val options =
            Vgo.Options(
                printVersion = printVersion,
                printStats = printStats,
                indent = indent,
                output = outputs,
                format = format,
                noOptimization = noOptimization,
                input = inputs,
            )

        return Vgo(options).run()
    }

    companion object {
        private val HELP_MESSAGE =
            """
> vgo [options] [file/directory]

Options:
  -h --help          print this message
  -o --output        file or directory, if not provided the input will be overwritten
  -s --stats         print statistics on processed files to standard out
  -v --version       print the version number
  --indent value     write files with value columns of indentation
  --format value     write specified output format (svg, vd, iv)
  --no-optimization  skip graphic optimization
            """.trimIndent()

        @JvmStatic
        fun main(args: Array<String>): Unit = exitProcess(CommandLineInterface().run(args))
    }
}
