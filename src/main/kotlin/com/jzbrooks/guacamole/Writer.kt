package com.jzbrooks.guacamole

interface Writer {
    val options: Set<Option>

    enum class Option {
        INDENT
    }
}