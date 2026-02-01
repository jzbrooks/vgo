package com.jzbrooks.vgo.util

import java.io.OutputStream

class CountingOutputStream : OutputStream() {
    var size: ULong = 0u

    override fun write(b: Int) {
        size++
    }

    override fun write(
        b: ByteArray,
        off: Int,
        len: Int,
    ) {
        size += len.toULong()
    }
}
