package com.jzbrooks.avdo.graphic.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test

class ParserTests {
    @Test
    fun testParseMultipleCommandInstructions() {
        val moveToString = "M1,1 L2,5 V3 Z"

        val commands = CommandString(moveToString).toCommandList()

        assertThat(commands[0])
                .isEqualTo(MoveTo(listOf(1 to 1)))
        assertThat(commands[1])
                .isEqualTo(LineTo(listOf(2 to 5)))
        assertThat(commands[2])
                .isEqualTo(VerticalLineTo(listOf(3)))
        assertThat(commands[3])
                .isInstanceOf(ClosePath::class.java)
    }
}