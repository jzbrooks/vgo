package com.jzbrooks.vgo.core.optimization

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.containsNone
import assertk.assertions.doesNotContain
import assertk.assertions.index
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.key
import assertk.assertions.prop
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo
import com.jzbrooks.vgo.core.util.element.createPath
import com.jzbrooks.vgo.core.util.math.Matrix3
import com.jzbrooks.vgo.core.util.math.Point
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class BakeTransformationsTests {
    private val bake = BakeTransformations()

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

        bake.visit(group)

        assertThat(group.foreign.keys, "foreign keys").containsNone("android:translateX", "android:translateY")
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

        bake.visit(group)

        assertThat(group::foreign).key("android:translateX").isEqualTo("@integer/translating_thing")
    }

    @Test
    fun `Resource valued transforms prevent group elision`() {
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

        bake.visit(group.elements.first() as Group)
        bake.visit(group)

        val originalNestedGroup = group.elements.first() as Group

        assertThat(group::foreign).doesNotContain("android:translateX", "15")
        assertThat(originalNestedGroup::foreign).contains("android:translateX", "@integer/translating_thing")
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

        bake.visit(group)

        assertThat(group.foreign.keys, "foreign keys").containsNone("android:translateX", "android:translateY")
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
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 4f))),
                    ),
                )
            ),
            null,
            mutableMapOf(),
            transform,
        )

        bake.visit(group)

        assertThat(group::elements).index(0)
            .isInstanceOf(Path::class)
            .prop(Path::commands)
            .containsExactly(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(24f, 24f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(54f, 18f))),
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

        bake.visit(group)

        assertThat(group::elements).index(0)
            .isInstanceOf(Path::class)
            .prop(Path::commands)
            .containsExactly(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(24f, 24f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(28f, 28f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(18f, 18f))),
                ClosePath,
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

        bake.visit(group)

        assertThat(group::elements).index(0)
            .isInstanceOf(Path::class)
            .prop(Path::commands).all {
                index(0).isInstanceOf(MoveTo::class).prop(MoveTo::parameters).index(0).all {
                    prop(Point::x).isCloseTo(-10f, 0.001f)
                    prop(Point::y).isCloseTo(10f, 0.001f)
                }

                index(1).isInstanceOf(LineTo::class).prop(LineTo::parameters).index(0).all {
                    prop(Point::x).isCloseTo(-11f, 0.001f)
                    prop(Point::y).isCloseTo(11f, 0.001f)
                }
            }
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

        bake.visit(group)

        assertThat(group::elements).index(0)
            .isInstanceOf(Path::class)
            .prop(Path::commands).all {
                index(0).isEqualTo(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))))

                index(1).isInstanceOf(LineTo::class).all {
                    prop(LineTo::variant).isEqualTo(CommandVariant.ABSOLUTE)
                    prop(LineTo::parameters).index(0).all {
                        prop(Point::x).isCloseTo(13.863f, 0.001f)
                        prop(Point::y).isCloseTo(11.035f, 0.001f)
                    }
                }

                index(2).isInstanceOf(LineTo::class).all {
                    prop(LineTo::variant).isEqualTo(CommandVariant.ABSOLUTE)
                    prop(LineTo::parameters).index(0).all {
                        prop(Point::x).isCloseTo(12.828f, 0.001f)
                        prop(Point::y).isCloseTo(14.899f, 0.001f)
                    }
                }

                index(3).isInstanceOf(LineTo::class).all {
                    prop(LineTo::variant).isEqualTo(CommandVariant.ABSOLUTE)
                    prop(LineTo::parameters).index(0).all {
                        prop(Point::x).isCloseTo(8.965f, 0.001f)
                        prop(Point::y).isCloseTo(13.864f, 0.001f)
                    }
                }
            }
    }
}
