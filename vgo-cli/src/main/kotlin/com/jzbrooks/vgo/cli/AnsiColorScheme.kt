package com.jzbrooks.vgo.cli

import com.jzbrooks.vgo.core.util.ir.IrColorScheme

private const val ESC = ""
private const val RESET = "$ESC[0m"

object AnsiColorScheme : IrColorScheme {
    override fun bold(s: String) = "$ESC[1m$s$RESET"

    override fun cyan(s: String) = "$ESC[36m$s$RESET"

    override fun green(s: String) = "$ESC[32m$s$RESET"

    override fun yellow(s: String) = "$ESC[33m$s$RESET"

    override fun dim(s: String) = "$ESC[2m$s$RESET"

    override fun colorSwatch(
        red: Int,
        green: Int,
        blue: Int,
    ) = "$ESC[38;2;$red;$green;${blue}m■$RESET"
}
