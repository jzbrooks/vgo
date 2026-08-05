package com.jzbrooks.vgo.cli

import com.jzbrooks.vgo.Vgo
import com.jzbrooks.vgo.core.util.ir.PlainColorScheme
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
        val printIrMode = argReader.readOptionWithDefault("print-ir", "auto")
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
        val check = argReader.readFlag("check")
        val readStdin = argReader.readFlag("stdin")

        if (format == null && noOptimization) {
            System.err.println("Warning: skipping optimization without --format is a no-op.")
        }

        if (check) {
            if (outputs.isNotEmpty()) {
                System.err.println("Error: --check cannot be combined with --output.")
                return 64
            }

            if (format != null) {
                System.err.println("Error: --check cannot be combined with --format.")
                return 64
            }

            if (printIrMode != null) {
                System.err.println("Error: --check cannot be combined with --print-ir.")
                return 64
            }

            if (printStats) {
                System.err.println("Warning: --stats is ignored in check mode.")
            }
        }

        val arguments = argReader.readArguments()

        if (readStdin && arguments.isNotEmpty()) {
            return inputError("--stdin cannot be combined with file or directory arguments.")
        }

        if (readStdin && outputs.isNotEmpty()) {
            return inputError("--stdin cannot be combined with --output.")
        }

        if (!readStdin && arguments.isEmpty() && !printVersion) {
            return inputError("No input files or directories were provided.")
        }

        val inputs =
            if (readStdin) {
                generateSequence(::readlnOrNull).toList()
            } else {
                arguments
            }

        val options =
            Vgo.Options(
                printVersion = printVersion,
                printStats = printStats,
                indent = indent,
                output = outputs,
                format = format,
                noOptimization = noOptimization,
                input = inputs,
                checkOnly = check,
                dumpIr =
                    when (printIrMode) {
                        "auto" -> {
                            Vgo.Options.IrDumpOptions(if (colorEnabled()) AnsiColorScheme else PlainColorScheme)
                        }

                        "plain" -> {
                            Vgo.Options.IrDumpOptions(PlainColorScheme)
                        }

                        "color" -> {
                            Vgo.Options.IrDumpOptions(AnsiColorScheme)
                        }

                        null -> {
                            null
                        }

                        else -> {
                            System.err.println("Warning: unsupported ir dump mode $printIrMode")
                            null
                        }
                    },
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
  --stdin            read newline-delimited file paths from standard input
  --check            verify inputs are fully shrunk without writing; prints files that would change and exits non-zero
  --print-ir[=MODE]  print IR tree and exit without writing (auto [default], color, plain; use = to pass mode)
            """.trimIndent()

        private fun inputError(message: String): Int {
            System.err.println("Error: $message")
            System.err.println()
            System.err.println(HELP_MESSAGE)
            return 64
        }

        @JvmStatic
        fun main(args: Array<String>): Unit = exitProcess(CommandLineInterface().run(args))
    }
}
