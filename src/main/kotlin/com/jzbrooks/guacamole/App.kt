package com.jzbrooks.guacamole

import com.jzbrooks.guacamole.optimization.*
import com.jzbrooks.guacamole.vd.VectorDrawableWriter
import com.jzbrooks.guacamole.vd.parse
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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

fun main(args: Array<String>) {
    val argReader = ArgReader(args.toMutableList())

    val writerOptions = if (argReader.readFlag("indent|i")) {
        setOf(Writer.Option.INDENT)
    } else {
        emptySet()
    }

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
        FileInputStream(input).use { inputStream ->
            val bytes = inputStream.readBytes()
            val sizeBefore = bytes.size

            val vectorDrawable = ByteArrayInputStream(bytes).use(::parse)

            orchestrator.optimize(vectorDrawable)

            val sizeAfter = ByteArrayOutputStream().use {
                writer.write(vectorDrawable, it)
                it.size()
            }

            println("Size before: $sizeBefore")
            println("Size after: $sizeAfter")
            println("Percent saved: ${((sizeBefore - sizeAfter) / sizeBefore.toDouble()) * 100}")

            if (!output.exists()) output.createNewFile()

            output.outputStream().use {
                writer.write(vectorDrawable, it)
            }
        }
    }
}
