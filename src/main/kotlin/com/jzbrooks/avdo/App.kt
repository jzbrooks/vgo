package com.jzbrooks.avdo

import com.jzbrooks.avdo.vd.VectorDrawableWriter
import com.jzbrooks.avdo.vd.parse
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream

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
        val reader = parse(inputStream)

        ByteArrayOutputStream().use {
            writer.write(reader, it)
            println(it.toString())
        }

        if (nonOptionArgs.size > 1) {
            FileOutputStream(nonOptionArgs[1]).use {
                writer.write(reader, it)
            }
        }
    }
}
