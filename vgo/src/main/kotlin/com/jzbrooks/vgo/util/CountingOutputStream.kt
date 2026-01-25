package com.jzbrooks.vgo.util

import java.io.OutputStream

class CountingOutputStream : OutputStream() {
    var size: ULong = 0u

    override fun write(b: Int) {
        size++
    }
}
