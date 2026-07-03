package com.jzbrooks.vgo.cli

internal fun colorEnabled(
    env: (String) -> String? = System::getenv,
    stdoutIsTerminal: () -> Boolean = ::stdoutIsTerminal,
): Boolean {
    if (!env("NO_COLOR").isNullOrEmpty()) return false
    val force = env("CLICOLOR_FORCE")
    if (!force.isNullOrEmpty() && force != "0") return true
    if (env("TERM") == "dumb") return false
    return stdoutIsTerminal()
}

internal fun stdoutIsTerminal(): Boolean {
    // JDK 25 Release Notes, JDK-8308591: JLine as the default Console provider

    // - The null guard matters most on ≤ 21 and 25+, where a redirected process simply has no console.
    val console = System.console() ?: return false

    // The isTerminal call matters most on 22–24, where a console object
    // exists even when piped — the null check alone would spray ANSI into
    // redirected output there. And on 25+ it's still not dead code: the
    // JLine provider remains opt-in-able, so "console exists, but it's
    // not a terminal" is still a reachable state.
    return if (Runtime.version().feature() >= 22) {
        java.io.Console::class.java.getMethod("isTerminal").invoke(console) as Boolean
    } else {
        true
    }
}
