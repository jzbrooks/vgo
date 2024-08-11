package com.jzbrooks.vgo.core.util.math

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class LineSegmentTests {
    @MethodSource
    @ParameterizedTest
    fun testIntersections(intersection: Intersection) {
        val (first, second, result) = intersection

        val actual = first.intersection(second)

        assertThat(actual).isNotNull().all {
            prop(Point::x).isCloseTo(result.x, 1e-3f)
            prop(Point::y).isCloseTo(result.y, 1e-3f)
        }
    }

    @MethodSource
    @ParameterizedTest
    fun testParallelLinesReturnsNull(pair: Pair<LineSegment, LineSegment>) {
        val (first, second) = pair

        val actual = first.intersection(second)

        assertThat(actual).isNull()
    }

    @MethodSource
    @ParameterizedTest
    fun testNonParallelDisjointSegmentsReturnsNull(pair: Pair<LineSegment, LineSegment>) {
        val (first, second) = pair

        val actual = first.intersection(second)

        assertThat(actual).isNull()
    }

    data class Intersection(val first: LineSegment, val second: LineSegment, val result: Point)

    companion object {
        @JvmStatic
        fun testIntersections(): List<Intersection> {
            return listOf(
                Intersection(
                    LineSegment(Point(0f, 10f), Point(0f, -10f)),
                    LineSegment(Point(-1f, 0f), Point(1f, 0f)),
                    Point.ZERO,
                ),
                Intersection(
                    LineSegment(Point(-1f, 2f), Point(2f, -2f)),
                    LineSegment(Point(-1f, -2f), Point(2f, 2f)),
                    Point(0.5f, 0f),
                ),
            )
        }

        @JvmStatic
        fun testParallelLinesReturnsNull(): List<Pair<LineSegment, LineSegment>> {
            return listOf(
                LineSegment(Point(0f, 3f), Point(0f, 3f)) to LineSegment(Point(1f, 3f), Point(1f, 3f)),
            )
        }

        @JvmStatic
        fun testNonParallelDisjointSegmentsReturnsNull(): List<Pair<LineSegment, LineSegment>> {
            return listOf(
                LineSegment(Point(1f, 1f), Point(4f, 4f)) to LineSegment(Point(1f, 8f), Point(2f, 4f))
            )
        }
    }
}
