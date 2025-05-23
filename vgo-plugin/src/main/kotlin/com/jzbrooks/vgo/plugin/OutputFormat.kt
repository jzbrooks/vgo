package com.jzbrooks.vgo.plugin

enum class OutputFormat(
    internal val cliName: String,
) {
    SVG("svg"),
    VECTOR_DRAWABLE("vd"),
    IMAGE_VECTOR("iv"),
    UNCHANGED("unchanged"),
}
