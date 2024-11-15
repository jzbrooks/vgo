package com.jzbrooks.vgo.core.util.math

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
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
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 5f))),
                LineTo(CommandVariant.RELATIVE, listOf(Point(10f, 5f))),
                LineTo(CommandVariant.RELATIVE, listOf(Point(10f, 5f))),
                HorizontalLineTo(CommandVariant.RELATIVE, listOf(10f)),
                VerticalLineTo(CommandVariant.RELATIVE, listOf(5f)),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isEqualTo(10f)
            prop(Rectangle::top).isEqualTo(20f)
            prop(Rectangle::right).isEqualTo(40f)
            prop(Rectangle::bottom).isEqualTo(5f)
        }
    }
}
