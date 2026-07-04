package com.jzbrooks.vgo.core.util.ir

interface IrColorScheme {
    fun bold(s: String): CharSequence

    fun cyan(s: String): CharSequence

    fun green(s: String): CharSequence

    fun yellow(s: String): CharSequence

    fun dim(s: String): CharSequence

    /** Returns an ANSI swatch prefix for the given RGB color, or empty string for no swatch. */
    fun colorSwatch(
        red: Int,
        green: Int,
        blue: Int,
    ): CharSequence
}

object PlainColorScheme : IrColorScheme {
    override fun bold(s: String) = s

    override fun cyan(s: String) = s

    override fun green(s: String) = s

    override fun yellow(s: String) = s

    override fun dim(s: String) = s

    override fun colorSwatch(
        red: Int,
        green: Int,
        blue: Int,
    ) = ""
}
