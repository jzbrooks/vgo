package com.jzbrooks.vgo.cli

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class ColorSupportTests {
    @Test
    fun `color is enabled for a terminal`() {
        val enabled = colorEnabled(env = { null }, stdoutIsTerminal = { true })
        assertThat(enabled).isTrue()
    }

    @Test
    fun `color is disabled when stdout is not a terminal`() {
        val enabled = colorEnabled(env = { null }, stdoutIsTerminal = { false })
        assertThat(enabled).isFalse()
    }

    @Test
    fun `no_color disables color for a terminal`() {
        val enabled =
            colorEnabled(
                env = { if (it == "NO_COLOR") "1" else null },
                stdoutIsTerminal = { true },
            )
        assertThat(enabled).isFalse()
    }

    @Test
    fun `no_color takes precedence over clicolor_force`() {
        val enabled =
            colorEnabled(
                env = {
                    when (it) {
                        "NO_COLOR" -> "1"
                        "CLICOLOR_FORCE" -> "1"
                        else -> null
                    }
                },
                stdoutIsTerminal = { true },
            )
        assertThat(enabled).isFalse()
    }

    @Test
    fun `empty no_color does not disable color`() {
        val enabled =
            colorEnabled(
                env = { if (it == "NO_COLOR") "" else null },
                stdoutIsTerminal = { true },
            )
        assertThat(enabled).isTrue()
    }

    @Test
    fun `clicolor_force enables color when stdout is not a terminal`() {
        val enabled =
            colorEnabled(
                env = { if (it == "CLICOLOR_FORCE") "1" else null },
                stdoutIsTerminal = { false },
            )
        assertThat(enabled).isTrue()
    }

    @Test
    fun `zero clicolor_force does not force color`() {
        val enabled =
            colorEnabled(
                env = { if (it == "CLICOLOR_FORCE") "0" else null },
                stdoutIsTerminal = { false },
            )
        assertThat(enabled).isFalse()
    }

    @Test
    fun `dumb terminal disables color`() {
        val enabled =
            colorEnabled(
                env = { if (it == "TERM") "dumb" else null },
                stdoutIsTerminal = { true },
            )
        assertThat(enabled).isFalse()
    }
}
