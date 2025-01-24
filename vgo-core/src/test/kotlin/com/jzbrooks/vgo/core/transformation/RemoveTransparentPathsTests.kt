package com.jzbrooks.vgo.core.transformation

import assertk.assertThat
import assertk.assertions.hasSize
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.util.element.createGraphic
import com.jzbrooks.vgo.core.util.element.createPath
import org.junit.jupiter.api.Test

class RemoveTransparentPathsTests {
    @Test
    fun testTransparentPathsAreRemoved() {
        val graphic =
            createGraphic(
                listOf(
                    createPath(
                        fill = Colors.TRANSPARENT,
                        stroke = Colors.TRANSPARENT,
                    ),
                    createPath(),
                    createPath(),
                ),
            )

        RemoveTransparentPaths().visit(graphic)

        assertThat(graphic::elements, "graphic elements").hasSize(2)
    }

    @Test
    fun testTransparentPathsWithIdsAreNotRemoved() {
        val graphic =
            createGraphic(
                listOf(
                    createPath(
                        id = "animatable",
                        fill = Colors.TRANSPARENT,
                        stroke = Colors.TRANSPARENT,
                    ),
                    createPath(),
                    createPath(),
                ),
            )

        RemoveTransparentPaths().visit(graphic)

        assertThat(graphic::elements, "graphic elements").hasSize(3)
    }

    @Test
    fun testTransparentPathsWithForeignColorsAreNotRemoved() {
        val graphic =
            createGraphic(
                listOf(
                    createPath(
                        fill = Colors.TRANSPARENT,
                        stroke = Colors.TRANSPARENT,
                        foreign = mutableMapOf("android:strokeColor" to "?attrs/dark"),
                    ),
                    createPath(),
                    createPath(),
                ),
            )

        RemoveTransparentPaths().visit(graphic)

        assertThat(graphic::elements, "graphic elements").hasSize(3)
    }
}
