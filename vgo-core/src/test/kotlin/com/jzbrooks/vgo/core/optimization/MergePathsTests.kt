package com.jzbrooks.vgo.core.optimization

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.PathElement
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.SmoothCubicBezierCurve
import com.jzbrooks.vgo.core.util.element.createPath
import com.jzbrooks.vgo.core.util.math.Point
import org.junit.jupiter.api.Test

class MergePathsTests {

    @Test
    fun testMergeSeveralPathsIntoOne() {
        val paths = listOf(
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))))
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))))
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))))
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f))))
            )
        )

        val graphic = object : Graphic {
            override var elements: List<Element> = paths
            override val id: String? = null
            override val foreign: MutableMap<String, String> = mutableMapOf()
        }

        val optimization = MergePaths()
        optimization.optimize(graphic)

        assertThat(graphic.elements.first()).isEqualTo(
            createPath(
                listOf(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))),
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))
                )
            ),
        )
    }

    @Test
    fun testMergeGroupPaths() {
        val paths = listOf(
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))))
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))))
            ),
            createPath(
                listOf(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 20f))),
                    SmoothCubicBezierCurve(CommandVariant.RELATIVE, listOf(SmoothCubicBezierCurve.Parameter(Point(20f, 10f), Point(20f, 20f)))),
                    LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))
                )
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))),
                strokeWidth = 5u,
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))))
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f))))
            )
        )

        val group = Group(paths)

        val graphic = object : Graphic {
            override var elements: List<Element> = listOf(group)
            override val id: String? = null
            override val foreign: MutableMap<String, String> = mutableMapOf()
        }

        val optimization = MergePaths()
        optimization.optimize(graphic)

        assertThat(graphic.elements).isEqualTo(
            listOf(
                Group(
                    listOf(
                        createPath(
                            listOf(
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 20f))),
                                SmoothCubicBezierCurve(CommandVariant.RELATIVE, listOf(SmoothCubicBezierCurve.Parameter(Point(20f, 10f), Point(20f, 20f)))),
                                LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                            ),
                        ),
                        createPath(
                            listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))),
                            strokeWidth = 5u,
                        ),
                        createPath(
                            listOf(
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))),
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))
                            ),
                        )
                    )
                )
            )
        )
    }

    @Test
    fun testMergePaths() {
        val paths = listOf(
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))))
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))))
            ),
            createPath(
                listOf(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 20f))),
                    SmoothCubicBezierCurve(CommandVariant.RELATIVE, listOf(SmoothCubicBezierCurve.Parameter(Point(20f, 10f), Point(20f, 20f)))),
                    LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))
                )
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))),
                strokeWidth = 5u,
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))))
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f))))
            )
        )

        val graphic = object : Graphic {
            override var elements: List<Element> = paths
            override val id: String? = null
            override val foreign: MutableMap<String, String> = mutableMapOf()
        }

        val optimization = MergePaths()
        optimization.optimize(graphic)

        assertThat(graphic.elements).isEqualTo(
            listOf(
                createPath(
                    listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 20f))),
                        SmoothCubicBezierCurve(CommandVariant.RELATIVE, listOf(SmoothCubicBezierCurve.Parameter(Point(20f, 10f), Point(20f, 20f)))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))
                    )
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))),
                    strokeWidth = 5u
                ),
                createPath(
                    listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))
                    )
                ),
            )
        )
    }

    @Test
    fun testMergePathsWithMixedElements() {
        val paths = listOf(
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))))
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))))
            ),
            createPath(
                listOf(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 20f))),
                    SmoothCubicBezierCurve(CommandVariant.RELATIVE, listOf(SmoothCubicBezierCurve.Parameter(Point(20f, 10f), Point(20f, 20f)))),
                    LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))
                )
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))),
                strokeWidth = 5u,
            ),
            Group(emptyList()),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))))
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f))))
            )
        )

        val graphic = object : Graphic {
            override var elements: List<Element> = paths
            override val id: String? = null
            override val foreign: MutableMap<String, String> = mutableMapOf()
        }

        val optimization = MergePaths()
        optimization.optimize(graphic)

        assertThat(graphic.elements).isEqualTo(
            listOf(
                createPath(
                    listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 20f))),
                        SmoothCubicBezierCurve(CommandVariant.RELATIVE, listOf(SmoothCubicBezierCurve.Parameter(Point(20f, 10f), Point(20f, 20f)))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))
                    )
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 30f)))),
                    strokeWidth = 5u,
                ),
                Group(emptyList()),
                createPath(
                    listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))
                    )
                ),
            )
        )
    }

    @Test
    fun testMergePathsWithMixedPathElements() {
        val paths = listOf(
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))))
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))))
            ),
            PseudoPath(listOf<Command>(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 40f))))),
            PseudoPath(listOf<Command>(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 40f))))),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))))
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f))))
            )
        )

        val graphic = object : Graphic {
            override var elements: List<Element> = paths
            override val id: String? = null
            override val foreign: MutableMap<String, String> = mutableMapOf()
        }

        val optimization = MergePaths()
        optimization.optimize(graphic)

        assertThat(graphic.elements).isEqualTo(
            listOf(
                createPath(
                    listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))
                    )
                ),
                PseudoPath(
                    listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 40f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 40f)))
                    )
                ),
                createPath(
                    listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f)))
                    )
                ),
            )
        )
    }

    @Test
    fun testMergePathsWithAttributesAfterMergedPathElement() {
        val paths = listOf(
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))))
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))))
            ),
            PseudoPath(listOf<Command>(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 40f))))),
            PseudoPath(listOf<Command>(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 40f))))),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f)))),
                fill = Color(0xFF0011FFu),
            ),
            createPath(
                listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f))))
            )
        )

        val graphic = object : Graphic {
            override var elements: List<Element> = paths
            override val id: String? = null
            override val foreign: MutableMap<String, String> = mutableMapOf()
        }

        val optimization = MergePaths()
        optimization.optimize(graphic)

        assertThat(graphic.elements).isEqualTo(
            listOf(
                createPath(
                    listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f)))
                    )
                ),
                PseudoPath(
                    listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(20f, 40f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(30f, 40f)))
                    )
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 40f)))),
                    fill = Color(0xFF0011FFu),
                ),
                createPath(
                    listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 50f), Point(10f, 10f), Point(20f, 30f), Point(40f, 0f))))
                )
            )
        )
    }

    data class PseudoPath(
        override var commands: List<Command>,
        override val id: String? = null,
        override val foreign: MutableMap<String, String> = mutableMapOf(),
    ) : PathElement {
        override fun hasSameAttributes(other: PathElement): Boolean {
            return other is PseudoPath && id == other.id && foreign == other.foreign
        }
    }
}
