package com.jzbrooks.vgo.vd.optimization

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.doesNotContain
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo
import com.jzbrooks.vgo.core.util.math.Matrix3
import com.jzbrooks.vgo.core.util.math.Point
import com.jzbrooks.vgo.util.assertk.containsKey
import com.jzbrooks.vgo.util.assertk.doesNotContainKey
import com.jzbrooks.vgo.util.element.createPath
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class BakeTransformationsTests {
    @Test
    fun testAvoidCrashIfParsedPathDataDoesNotExist() {
        val transform = Matrix3.from(
            arrayOf(
                floatArrayOf(1f, 0f, 14f),
                floatArrayOf(0f, 1f, 14f),
                floatArrayOf(0f, 0f, 1f),
            )
        )
        val group = Group(
            listOf(createPath()),
            null,
            mutableMapOf(),
            transform,
        )

        BakeTransformations().optimize(object : Graphic {
            override var elements: List<Element> = listOf(group)
            override val id: String? = null
            override val foreign: MutableMap<String, String> = mutableMapOf()
        })

        assertThat(group.foreign).doesNotContainKey("android:translateX")
        assertThat(group.foreign).doesNotContainKey("android:translateY")
    }

    @Test
    fun testAvoidCrashIfTransformsAreSpecifiedByResources() {
        val group = Group(
            listOf(
                createPath(
                    listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 4f)))
                    ),
                )
            ),
            null,
            mutableMapOf("android:translateX" to "@integer/translating_thing"),
            Matrix3.IDENTITY,
        )

        BakeTransformations().optimize(object : Graphic {
            override var elements: List<Element> = listOf(group)
            override val id: String? = null
            override val foreign: MutableMap<String, String> = mutableMapOf()
        })

        assertThat(group.foreign).containsKey("android:translateX")
    }

    @Test
    fun testAvoidCrashIfASharedTransformIsSpecifiedByResource() {
        val transform = Matrix3.from(
            arrayOf(
                floatArrayOf(1f, 0f, 15f),
                floatArrayOf(0f, 1f, 0f),
                floatArrayOf(0f, 0f, 1f),
            )
        )

        val group = Group(
            listOf(
                Group(
                    listOf(
                        createPath(
                            listOf(
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                                LineTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 4f)))
                            ),
                        )
                    ),
                    null,
                    mutableMapOf("android:translateX" to "@integer/translating_thing"),
                    Matrix3.IDENTITY,
                )
            ),
            null,
            mutableMapOf(),
            transform,
        )

        BakeTransformations().optimize(object : Graphic {
            override var elements: List<Element> = listOf(group)
            override val id: String? = null
            override val foreign: MutableMap<String, String> = mutableMapOf()
        })

        val insertedGroup = group.elements.first() as Group
        val originalNestedGroup = insertedGroup.elements.first() as Group

        assertThat(group.foreign).doesNotContain("android:translateX", "15")
        assertThat(insertedGroup.foreign).contains("android:translateX", "@integer/translating_thing")
        assertThat(originalNestedGroup.foreign).doesNotContain("android:translateX", "@integer/translating_thing")
    }

    @Test
    fun testTransformationAttributesRemoved() {
        val transform = Matrix3.from(
            arrayOf(
                floatArrayOf(1f, 0f, 14f),
                floatArrayOf(0f, 1f, 14f),
                floatArrayOf(0f, 0f, 1f),
            )
        )

        val group = Group(
            listOf(
                createPath(
                    listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 4f)))
                    ),
                )
            ),
            null,
            mutableMapOf(),
            transform,
        )

        BakeTransformations().optimize(object : Graphic {
            override var elements: List<Element> = listOf(group)
            override val id: String? = null
            override val foreign: MutableMap<String, String> = mutableMapOf()
        })

        assertThat(group.foreign).doesNotContainKey("android:translateX")
        assertThat(group.foreign).doesNotContainKey("android:translateY")
    }

    @Test
    fun testAncestorGroupTransformationAppliedToPathElements() {
        val transform = Matrix3.from(
            arrayOf(
                floatArrayOf(1f, 0f, 14f),
                floatArrayOf(0f, 1f, 14f),
                floatArrayOf(0f, 0f, 1f),
            )
        )

        val group = Group(
            listOf(
                createPath(
                    listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 4f)))
                    ),
                )
            ),
            null,
            mutableMapOf(),
            transform,
        )

        BakeTransformations().optimize(object : Graphic {
            override var elements: List<Element> = listOf(group)
            override val id: String? = null
            override val foreign: MutableMap<String, String> = mutableMapOf()
        })

        val path = group.elements.first() as Path
        assertThat(path.commands)
            .containsExactly(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(24f, 24f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(54f, 18f)))
            )
    }

    @Test
    fun testBakeHandlesRelativeCommands() {
        val transform = Matrix3.from(
            arrayOf(
                floatArrayOf(1f, 0f, 14f),
                floatArrayOf(0f, 1f, 14f),
                floatArrayOf(0f, 0f, 1f),
            )
        )

        val group = Group(
            listOf(
                createPath(
                    listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                        LineTo(CommandVariant.RELATIVE, listOf(Point(4f, 4f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(4f, 4f))),
                        ClosePath,
                    ),
                )
            ),
            null,
            mutableMapOf(),
            transform,
        )

        BakeTransformations().optimize(object : Graphic {
            override var elements: List<Element> = listOf(group)
            override val id: String? = null
            override val foreign: MutableMap<String, String> = mutableMapOf()
        })

        val path = group.elements.first() as Path
        assertThat(path.commands)
            .containsExactly(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(24f, 24f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(28f, 28f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(18f, 18f))),
                ClosePath
            )
    }

    @Test
    fun testGroupRotationApplied() {
        val rad = (90.0 * PI / 180.0).toFloat()
        val rotationMatrix = Matrix3.from(
            arrayOf(
                floatArrayOf(cos(rad), -sin(rad), 0f),
                floatArrayOf(sin(rad), cos(rad), 0f),
                floatArrayOf(0f, 0f, 1f),
            )
        )

        val group = Group(
            listOf(
                createPath(
                    listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                        LineTo(CommandVariant.RELATIVE, listOf(Point(1f, 1f)))
                    ),
                ),
            ),
            null,
            mutableMapOf(),
            rotationMatrix,
        )

        BakeTransformations().optimize(object : Graphic {
            override var elements: List<Element> = listOf(group)
            override val id: String? = null
            override val foreign: MutableMap<String, String> = mutableMapOf()
        })

        val path = group.elements.first() as Path
        val firstCommand = path.commands[0] as MoveTo
        val secondCommand = path.commands[1] as LineTo

        assertThat(firstCommand.parameters.first().x).isCloseTo(-10f, 0.001f)
        assertThat(firstCommand.parameters.first().y).isCloseTo(10f, 0.001f)
        assertThat(secondCommand.parameters.first().x).isCloseTo(-11f, 0.001f)
        assertThat(secondCommand.parameters.first().y).isCloseTo(11f, 0.001f)
    }

    @Test
    fun testGroupRotationAppliedWithSequentialRelativeCommands() {
        val rad = (15.0 * PI / 180.0).toFloat()
        val rotationMatrix = Matrix3.from(
            arrayOf(
                floatArrayOf(cos(rad), -sin(rad), 0f),
                floatArrayOf(sin(rad), cos(rad), 0f),
                floatArrayOf(0f, 0f, 1f),
            )
        )

        val pivot = Matrix3.from(
            arrayOf(
                floatArrayOf(1f, 0f, 10f),
                floatArrayOf(0f, 1f, 10f),
                floatArrayOf(0f, 0f, 1f),
            )
        )

        val pivotInverse = Matrix3.from(
            arrayOf(
                floatArrayOf(1f, 0f, -10f),
                floatArrayOf(0f, 1f, -10f),
                floatArrayOf(0f, 0f, 1f),
            )
        )

        val transform = pivot * rotationMatrix * pivotInverse

        val group = Group(
            listOf(
                createPath(
                    listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                        HorizontalLineTo(CommandVariant.RELATIVE, listOf(4f)),
                        VerticalLineTo(CommandVariant.RELATIVE, listOf(4f)),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 14f))),
                        ClosePath,
                    ),
                ),
            ),
            null,
            mutableMapOf(),
            transform,
        )

        BakeTransformations().optimize(object : Graphic {
            override var elements: List<Element> = listOf(group)
            override val id: String? = null
            override val foreign: MutableMap<String, String> = mutableMapOf()
        })

        val path = group.elements.first() as Path
        val firstCommand = path.commands[0] as MoveTo
        val secondCommand = path.commands[1] as LineTo
        val thirdCommand = path.commands[2] as LineTo
        val fourthCommand = path.commands[3] as LineTo

        assertThat(firstCommand).isEqualTo(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))))

        assertThat(secondCommand.variant).isEqualTo(CommandVariant.ABSOLUTE)
        assertThat(secondCommand.parameters.first().x).isCloseTo(13.863f, 0.001f)
        assertThat(secondCommand.parameters.first().y).isCloseTo(11.035f, 0.001f)

        assertThat(thirdCommand.variant).isEqualTo(CommandVariant.ABSOLUTE)
        assertThat(thirdCommand.parameters.first().x).isCloseTo(12.828f, 0.001f)
        assertThat(thirdCommand.parameters.first().y).isCloseTo(14.899f, 0.001f)

        assertThat(fourthCommand.variant).isEqualTo(CommandVariant.ABSOLUTE)
        assertThat(fourthCommand.parameters.first().x).isCloseTo(8.965f, 0.001f)
        assertThat(fourthCommand.parameters.first().y).isCloseTo(13.864f, 0.001f)
    }
}
