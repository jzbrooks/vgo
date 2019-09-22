package com.jzbrooks.guacamole

import com.jzbrooks.guacamole.optimization.*
import com.jzbrooks.guacamole.vd.VectorDrawableWriter
import com.jzbrooks.guacamole.vd.parse
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream

private val orchestrator = Orchestrator(
        listOf(
                CollapseGroups(),
                MergePaths(),
                CommandVariant(),
                RemoveEmptyGroups()
        )
)

fun main(args: Array<String>) {
    val path = args.first()
    val nonOptionArgs = args.filter { !it.startsWith("--") }
    val optionArgs = args.filter { it.startsWith("--") }
            .map { it.removePrefix("--") }
            .toSet()

    val writerOptions = if (optionArgs.contains("indent")) {
        setOf(Writer.Option.INDENT)
    } else {
        emptySet()
    }

    val writer = VectorDrawableWriter(writerOptions)

    FileInputStream(path).use { inputStream ->
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

        if (nonOptionArgs.size > 1) {
            FileOutputStream(nonOptionArgs[1]).use {
                writer.write(vectorDrawable, it)
            }
        }
    }
}
