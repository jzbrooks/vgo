package com.jzbrooks.avdo

import com.jzbrooks.avdo.vd.parse
import com.jzbrooks.avdo.vd.write
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream

fun main(args: Array<String>) {
    val (path, target) = args

    FileInputStream(path).use { inputStream ->
        val reader = parse(inputStream)

        ByteArrayOutputStream().use {
            write(reader, it)
            println(it.toString())
        }

        FileOutputStream(target).use {
            write(reader, it)
        }
    }
}
