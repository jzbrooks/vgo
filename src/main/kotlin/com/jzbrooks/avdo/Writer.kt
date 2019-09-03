package com.jzbrooks.avdo

interface Writer {
    val options: Set<Option>

    enum class Option {
        INDENT
    }
}