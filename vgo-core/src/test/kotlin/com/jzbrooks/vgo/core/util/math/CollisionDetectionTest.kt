package com.jzbrooks.vgo.core.util.math

import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo
import org.junit.jupiter.api.Test

class CollisionDetectionTest {
    @Test
    fun `Sample points`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 1f))),
                LineTo(CommandVariant.RELATIVE, listOf(Point(-9f, 6f))),
                LineTo(CommandVariant.RELATIVE, listOf(Point(3f, 7f))),
                HorizontalLineTo(CommandVariant.RELATIVE, listOf(10f)),
                VerticalLineTo(CommandVariant.RELATIVE, listOf(-4f)),
            )

        val sample = samplePerimeter(commands)
    }
}