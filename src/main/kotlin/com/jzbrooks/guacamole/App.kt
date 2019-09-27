package com.jzbrooks.guacamole

import com.jzbrooks.guacamole.optimization.*
import com.jzbrooks.guacamole.vd.VectorDrawableWriter
import com.jzbrooks.guacamole.vd.parse
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream

private val orchestrator = Orchestrator(
        listOf(
                CollapseGroups(),
                MergePaths(),
                CommandVariant(),
                RemoveEmptyGroups()
        )
)
private var printStats = false

fun main(args: Array<String>) {
    val argReader = ArgReader(args.toMutableList())

    val writerOptions = if (argReader.readFlag("indent|i")) {
        setOf(Writer.Option.INDENT)
    } else {
        emptySet()
    }

    printStats = argReader.readFlag("stats|s")

    val writer = VectorDrawableWriter(writerOptions)

    val outputs = run {
        val outputPaths = mutableListOf<String>()
        var output = argReader.readOption("output|o")
        while (output != null) {
            outputPaths.add(output)
            output = argReader.readOption("output|o")
        }
        outputPaths.toList()
    }

    val inputs = argReader.readArguments()

    val inputOutputPair = if (outputs.isNotEmpty()) {
        inputs.zip(outputs) { a, b ->
            Pair(File(a), File(b))
        }
    } else {
        inputs.zip(inputs) { a, b ->
            Pair(File(a), File(b))
        }
    }

    for ((input, output) in inputOutputPair) {
        if (input.isFile && (output.isFile || !output.exists())) {
            handleFile(input, output, writer)
        } else if (input.isDirectory && (output.isDirectory || !output.exists())) {
            input.listFiles { file -> !file.isHidden }?.forEach { handleFile(it, File(output, it.name), writer) }
        } else {
            System.err.println("Input and output must be either files or directories.")
            System.err.println("Input is a " + if (input.isFile) "file" else "directory")
            System.err.println("Output is a " + if (output.isFile) "file" else "directory")
        }
    }
}

private fun handleFile(input: File, output: File, writer: Writer) {
    FileInputStream(input).use { inputStream ->
        val sizeBefore = inputStream.channel.size()

        val vectorDrawable = ByteArrayInputStream(inputStream.readBytes()).use(::parse)

        orchestrator.optimize(vectorDrawable)

        if (!output.parentFile.exists()) output.parentFile.mkdirs()
        if (!output.exists()) output.createNewFile()

        output.outputStream().use {
            writer.write(vectorDrawable, it)

            if (printStats) {
                val sizeAfter = it.channel.size()
                val percentSaved = ((sizeBefore - sizeAfter) / sizeBefore.toDouble()) * 100
                println("Size before: $sizeBefore")
                println("Size after: $sizeAfter")
                println("Percent saved: $percentSaved")
            }
        }
    }
}
