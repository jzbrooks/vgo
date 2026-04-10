package com.jzbrooks.vgo.core.transformation

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
import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo
import com.jzbrooks.vgo.core.util.assertk.isEqualTo
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
        val transform = Matrix3.from(floatArrayOf(1f, 0f, 14f, 0f, 1f, 14f, 0f, 0f, 1f))
        val group =
            Group(
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
        val group =
            Group(
                listOf(
                    createPath(
                        listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                            LineTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 4f))),
                        ),
                    ),
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
        val transform = Matrix3.from(floatArrayOf(1f, 0f, 15f, 0f, 1f, 0f, 0f, 0f, 1f))

        val group =
            Group(
                listOf(
                    Group(
                        listOf(
                            createPath(
                                listOf(
                                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                                    LineTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 4f))),
                                ),
                            ),
                        ),
                        null,
                        mutableMapOf("android:translateX" to "@integer/translating_thing"),
                        Matrix3.IDENTITY,
                    ),
                ),
                null,
                mutableMapOf(),
                transform,
            )

        // Top-down: visit outer first, then inner
        bake.visit(group)
        bake.visit(group.elements.first() as Group)

        val originalNestedGroup = group.elements.first() as Group

        assertThat(group::foreign).doesNotContain("android:translateX", "15")
        assertThat(originalNestedGroup::foreign).contains("android:translateX", "@integer/translating_thing")
    }

    @Test
    fun testTransformationAttributesRemoved() {
        val transform = Matrix3.from(floatArrayOf(1f, 0f, 14f, 0f, 1f, 14f, 0f, 0f, 1f))

        val group =
            Group(
                listOf(
                    createPath(
                        listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                            LineTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 4f))),
                        ),
                    ),
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
        val transform =
            Matrix3.from(
                floatArrayOf(1f, 0f, 14f, 0f, 1f, 14f, 0f, 0f, 1f),
            )

        val group =
            Group(
                listOf(
                    createPath(
                        listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                            LineTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 4f))),
                        ),
                    ),
                ),
                null,
                mutableMapOf(),
                transform,
            )

        bake.visit(group)

        assertThat(group::elements)
            .index(0)
            .isInstanceOf(Path::class)
            .prop(Path::commands)
            .containsExactly(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(24f, 24f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(54f, 18f))),
            )
    }

    @Test
    fun testBakeHandlesRelativeCommands() {
        val transform =
            Matrix3.from(
                floatArrayOf(1f, 0f, 14f, 0f, 1f, 14f, 0f, 0f, 1f),
            )

        val group =
            Group(
                listOf(
                    createPath(
                        listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                            LineTo(CommandVariant.RELATIVE, listOf(Point(4f, 4f))),
                            LineTo(CommandVariant.ABSOLUTE, listOf(Point(4f, 4f))),
                            ClosePath,
                        ),
                    ),
                ),
                null,
                mutableMapOf(),
                transform,
            )

        bake.visit(group)

        assertThat(group::elements)
            .index(0)
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
    fun `relative commands with multiple parameters resolve each against the previous endpoint`() {
        val transform = Matrix3.from(floatArrayOf(1f, 0f, 14f, 0f, 1f, 14f, 0f, 0f, 1f))

        val group =
            Group(
                listOf(
                    createPath(
                        listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                            LineTo(CommandVariant.RELATIVE, listOf(Point(5f, 5f), Point(10f, 10f))),
                        ),
                    ),
                ),
                null,
                mutableMapOf(),
                transform,
            )

        bake.visit(group)

        assertThat(group::elements)
            .index(0)
            .isInstanceOf(Path::class)
            .prop(Path::commands)
            .containsExactly(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(24f, 24f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(29f, 29f), Point(39f, 39f))),
            )
    }

    @Test
    fun testGroupRotationApplied() {
        val rad = (90.0 * PI / 180.0).toFloat()
        val rotationMatrix = Matrix3.from(floatArrayOf(cos(rad), -sin(rad), 0f, sin(rad), cos(rad), 0f, 0f, 0f, 1f))

        val group =
            Group(
                listOf(
                    createPath(
                        listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                            LineTo(CommandVariant.RELATIVE, listOf(Point(1f, 1f))),
                        ),
                    ),
                ),
                null,
                mutableMapOf(),
                rotationMatrix,
            )

        bake.visit(group)

        assertThat(group::elements)
            .index(0)
            .isInstanceOf(Path::class)
            .prop(Path::commands)
            .all {
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
    fun `transform is applied to path children when group also contains non-relocatable nested groups`() {
        val transform = Matrix3.from(floatArrayOf(2f, 0f, 0f, 0f, 2f, 0f, 0f, 0f, 1f))

        val innerGroup =
            Group(
                listOf(
                    createPath(
                        listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(1f, 1f))),
                            LineTo(CommandVariant.ABSOLUTE, listOf(Point(2f, 2f))),
                        ),
                    ),
                ),
                "inner",
                mutableMapOf(),
                Matrix3.IDENTITY,
            )

        val outerGroup =
            Group(
                listOf(
                    createPath(
                        listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(3f, 3f))),
                            LineTo(CommandVariant.ABSOLUTE, listOf(Point(4f, 4f))),
                        ),
                    ),
                    innerGroup,
                ),
                null,
                mutableMapOf(),
                transform,
            )

        // Top-down: visit outer first, then inner
        bake.visit(outerGroup)
        bake.visit(innerGroup)

        assertThat(outerGroup::elements)
            .index(0)
            .isInstanceOf<Path>()
            .prop(Path::commands)
            .containsExactly(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(6f, 6f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(8f, 8f))),
            )

        assertThat(innerGroup::elements)
            .index(0)
            .isInstanceOf<Path>()
            .prop(Path::commands)
            .containsExactly(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(2f, 2f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(4f, 4f))),
            )
    }

    @Test
    fun `group children compose baked transformations of parent groups`() {
        val transform = Matrix3.from(floatArrayOf(2f, 0f, 0f, 0f, 2f, 0f, 0f, 0f, 1f))

        val innerGroup =
            Group(
                listOf(
                    createPath(
                        listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(1f, 1f))),
                            LineTo(CommandVariant.ABSOLUTE, listOf(Point(2f, 2f))),
                        ),
                    ),
                ),
                "inner",
                mutableMapOf(),
                Matrix3.IDENTITY,
            )

        val outerGroup =
            Group(
                listOf(
                    createPath(
                        listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(3f, 3f))),
                            LineTo(CommandVariant.ABSOLUTE, listOf(Point(4f, 4f))),
                        ),
                    ),
                    innerGroup,
                ),
                null,
                mutableMapOf(),
                transform,
            )

        // Top-down: visit outer first, composing transform onto inner
        bake.visit(outerGroup)

        assertThat(outerGroup::elements)
            .index(1)
            .isInstanceOf<Group>()
            .prop(Group::transform)
            .isEqualTo(transform)

        // Then visit inner, baking the composed transform into paths
        bake.visit(innerGroup)

        assertThat(innerGroup::transform).isEqualTo(Matrix3.IDENTITY)

        assertThat(innerGroup::elements)
            .index(0)
            .isInstanceOf<Path>()
            .prop(Path::commands)
            .containsExactly(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(2f, 2f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(4f, 4f))),
            )
    }

    @Test
    fun `transform is baked through nested group with identity transform`() {
        val translate = Matrix3.from(floatArrayOf(1f, 0f, 50f, 0f, 1f, 50f, 0f, 0f, 1f))

        val innerGroup =
            Group(
                listOf(
                    createPath(
                        listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                            LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 0f))),
                            LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                            LineTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 10f))),
                            ClosePath,
                        ),
                    ),
                ),
                null,
                mutableMapOf(),
                Matrix3.IDENTITY,
            )

        val outerGroup =
            Group(
                listOf(innerGroup),
                null,
                mutableMapOf(),
                translate,
            )

        // Top-down: visit outer first, then inner
        bake.visit(outerGroup)
        bake.visit(innerGroup)

        assertThat(outerGroup::transform).isEqualTo(Matrix3.IDENTITY)
        assertThat(innerGroup::transform).isEqualTo(Matrix3.IDENTITY)

        assertThat(innerGroup::elements)
            .index(0)
            .isInstanceOf<Path>()
            .prop(Path::commands)
            .containsExactly(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(60f, 50f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(60f, 60f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 60f))),
                ClosePath,
            )
    }

    @Test
    fun `nested group transforms are fully baked with top-down traversal`() {
        val outerTransform = Matrix3.from(floatArrayOf(1f, 0f, 10f, 0f, 1f, 10f, 0f, 0f, 1f))
        val innerTransform = Matrix3.from(floatArrayOf(2f, 0f, 0f, 0f, 2f, 0f, 0f, 0f, 1f))

        val innerGroup =
            Group(
                listOf(
                    createPath(
                        listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(1f, 1f))),
                            LineTo(CommandVariant.ABSOLUTE, listOf(Point(2f, 2f))),
                        ),
                    ),
                ),
                "inner",
                mutableMapOf(),
                innerTransform,
            )

        val outerGroup =
            Group(
                listOf(innerGroup),
                null,
                mutableMapOf(),
                outerTransform,
            )

        // Top-down: outer first, then inner
        bake.visit(outerGroup)
        bake.visit(innerGroup)

        assertThat(outerGroup::transform).isEqualTo(Matrix3.IDENTITY)
        assertThat(innerGroup::transform).isEqualTo(Matrix3.IDENTITY)

        // Path should have innerTransform applied first, then outerTransform composed on:
        // Original point (1,1) * scale(2) = (2,2), then * translate(10,10) = (12,12)
        // But composition: outerTransform * innerTransform * point
        // = translate(10,10) * scale(2) * (1,1)
        // = translate(10,10) * (2,2) = (12,12)
        assertThat(innerGroup::elements)
            .index(0)
            .isInstanceOf<Path>()
            .prop(Path::commands)
            .containsExactly(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(12f, 12f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(14f, 14f))),
            )
    }

    @Test
    fun `clip path children receive parent group transform`() {
        val transform = Matrix3.from(floatArrayOf(1f, 0f, 5f, 0f, 1f, 5f, 0f, 0f, 1f))

        val group =
            Group(
                listOf(
                    createPath(
                        listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                            LineTo(CommandVariant.ABSOLUTE, listOf(Point(1f, 1f))),
                        ),
                    ),
                    ClipPath(
                        listOf(
                            createPath(
                                listOf(
                                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                                    LineTo(CommandVariant.ABSOLUTE, listOf(Point(2f, 2f))),
                                ),
                            ),
                        ),
                        null,
                        mutableMapOf(),
                    ),
                ),
                null,
                mutableMapOf(),
                transform,
            )

        bake.visit(group)

        assertThat(group::transform).isEqualTo(Matrix3.IDENTITY)

        assertThat(group::elements)
            .index(0)
            .isInstanceOf<Path>()
            .prop(Path::commands)
            .containsExactly(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(5f, 5f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(6f, 6f))),
            )

        val clipPath = group.elements[1] as ClipPath
        assertThat(clipPath::elements)
            .index(0)
            .isInstanceOf<Path>()
            .prop(Path::commands)
            .containsExactly(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(5f, 5f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(7f, 7f))),
            )
    }

    @Test
    fun `clip path with non-path children prevents baking`() {
        val transform = Matrix3.from(floatArrayOf(1f, 0f, 5f, 0f, 1f, 5f, 0f, 0f, 1f))

        val group =
            Group(
                listOf(
                    ClipPath(
                        listOf(
                            Group(
                                listOf(createPath()),
                                null,
                                mutableMapOf(),
                                Matrix3.IDENTITY,
                            ),
                        ),
                        null,
                        mutableMapOf(),
                    ),
                ),
                null,
                mutableMapOf(),
                transform,
            )

        bake.visit(group)

        assertThat(group::transform).isEqualTo(transform)
    }

    @Test
    fun `relocatable child group with non-identity transform is not flattened`() {
        val childTransform = Matrix3.from(floatArrayOf(2f, 0f, 0f, 0f, 2f, 0f, 0f, 0f, 1f))

        val group =
            Group(
                listOf(
                    Group(
                        listOf(
                            createPath(
                                listOf(
                                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(1f, 1f))),
                                ),
                            ),
                        ),
                        null,
                        mutableMapOf(),
                        childTransform,
                    ),
                ),
                null,
                mutableMapOf(),
                Matrix3.IDENTITY,
            )

        bake.visit(group)

        // The child group should not be flattened because it has a non-identity transform
        assertThat(group::elements)
            .index(0)
            .isInstanceOf<Group>()
    }

    @Test
    fun testGroupRotationAppliedWithSequentialRelativeCommands() {
        val rad = (15.0 * PI / 180.0).toFloat()

        val rotationMatrix = Matrix3.from(floatArrayOf(cos(rad), -sin(rad), 0f, sin(rad), cos(rad), 0f, 0f, 0f, 1f))
        val pivot = Matrix3.from(floatArrayOf(1f, 0f, 10f, 0f, 1f, 10f, 0f, 0f, 1f))
        val pivotInverse = Matrix3.from(floatArrayOf(1f, 0f, -10f, 0f, 1f, -10f, 0f, 0f, 1f))

        val transform = pivot * rotationMatrix * pivotInverse

        val group =
            Group(
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

        assertThat(group::elements)
            .index(0)
            .isInstanceOf(Path::class)
            .prop(Path::commands)
            .all {
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
