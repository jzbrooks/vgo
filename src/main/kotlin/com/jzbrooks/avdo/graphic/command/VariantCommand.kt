package com.jzbrooks.avdo.graphic.command

interface VariantCommand : Command {
    val variant: Variant

    enum class Variant {
        ABSOLUTE,
        RELATIVE
    }
}
