package com.jzbrooks.vgo.core.util.math

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.hasSize
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CollisionTests {
    @Nested
    inner class ConvexHullTests {
        @Test
        fun `square points produce square hull`() {
            val points =
                listOf(
                    Point(0f, 0f),
                    Point(10f, 0f),
                    Point(10f, 10f),
                    Point(0f, 10f),
                )

            val hull = convexHull(points)

            assertThat(hull).hasSize(4)
            assertThat(hull).containsExactlyInAnyOrder(
                Point(0f, 0f),
                Point(10f, 0f),
                Point(10f, 10f),
                Point(0f, 10f),
            )
        }

        @Test
        fun `interior points are excluded from hull`() {
            val points =
                listOf(
                    Point(0f, 0f),
                    Point(10f, 0f),
                    Point(10f, 10f),
                    Point(0f, 10f),
                    Point(5f, 5f), // Interior point
                )

            val hull = convexHull(points)

            assertThat(hull).hasSize(4)
            assertThat(hull).containsExactlyInAnyOrder(
                Point(0f, 0f),
                Point(10f, 0f),
                Point(10f, 10f),
                Point(0f, 10f),
            )
        }

        @Test
        fun `triangle points produce triangle hull`() {
            val points =
                listOf(
                    Point(0f, 0f),
                    Point(10f, 0f),
                    Point(5f, 10f),
                )

            val hull = convexHull(points)

            assertThat(hull).hasSize(3)
            assertThat(hull).containsExactlyInAnyOrder(
                Point(0f, 0f),
                Point(10f, 0f),
                Point(5f, 10f),
            )
        }

        @Test
        fun `collinear points produce line hull`() {
            val points =
                listOf(
                    Point(0f, 0f),
                    Point(5f, 0f),
                    Point(10f, 0f),
                )

            val hull = convexHull(points)

            assertThat(hull).hasSize(2)
            assertThat(hull).containsExactlyInAnyOrder(
                Point(0f, 0f),
                Point(10f, 0f),
            )
        }
    }

    @Nested
    inner class GjkIntersectionTests {
        @Test
        fun `non-overlapping squares do not intersect`() {
            val hullA =
                listOf(
                    Point(0f, 0f),
                    Point(10f, 0f),
                    Point(10f, 10f),
                    Point(0f, 10f),
                )
            val hullB =
                listOf(
                    Point(20f, 0f),
                    Point(30f, 0f),
                    Point(30f, 10f),
                    Point(20f, 10f),
                )

            val intersects = intersects(hullA, hullB)

            assertThat(intersects).isFalse()
        }

        @Test
        fun `overlapping squares intersect`() {
            val hullA =
                listOf(
                    Point(0f, 0f),
                    Point(10f, 0f),
                    Point(10f, 10f),
                    Point(0f, 10f),
                )
            val hullB =
                listOf(
                    Point(5f, 5f),
                    Point(15f, 5f),
                    Point(15f, 15f),
                    Point(5f, 15f),
                )

            val intersects = intersects(hullA, hullB)

            assertThat(intersects).isTrue()
        }

        @Test
        fun `contained square intersects`() {
            val hullA =
                listOf(
                    Point(0f, 0f),
                    Point(20f, 0f),
                    Point(20f, 20f),
                    Point(0f, 20f),
                )
            val hullB =
                listOf(
                    Point(5f, 5f),
                    Point(15f, 5f),
                    Point(15f, 15f),
                    Point(5f, 15f),
                )

            val intersects = intersects(hullA, hullB)

            assertThat(intersects).isTrue()
        }

        @Test
        fun `touching edges intersect`() {
            val hullA =
                listOf(
                    Point(0f, 0f),
                    Point(10f, 0f),
                    Point(10f, 10f),
                    Point(0f, 10f),
                )
            val hullB =
                listOf(
                    Point(10f, 0f),
                    Point(20f, 0f),
                    Point(20f, 10f),
                    Point(10f, 10f),
                )

            val intersects = intersects(hullA, hullB)

            assertThat(intersects).isTrue()
        }

        @Test
        fun `triangles that do not overlap do not intersect`() {
            val hullA =
                listOf(
                    Point(0f, 0f),
                    Point(5f, 10f),
                    Point(10f, 0f),
                )
            val hullB =
                listOf(
                    Point(20f, 0f),
                    Point(25f, 10f),
                    Point(30f, 0f),
                )

            val intersects = intersects(hullA, hullB)

            assertThat(intersects).isFalse()
        }

        @Test
        fun `triangles that overlap intersect`() {
            val hullA =
                listOf(
                    Point(0f, 0f),
                    Point(5f, 10f),
                    Point(10f, 0f),
                )
            val hullB =
                listOf(
                    Point(3f, 2f),
                    Point(8f, 12f),
                    Point(13f, 2f),
                )

            val intersects = intersects(hullA, hullB)

            assertThat(intersects).isTrue()
        }

        @Test
        fun `empty hull does not intersect`() {
            val hullA = emptyList<Point>()
            val hullB =
                listOf(
                    Point(0f, 0f),
                    Point(10f, 0f),
                    Point(10f, 10f),
                )

            val intersects = intersects(hullA, hullB)

            assertThat(intersects).isFalse()
        }

        @Test
        fun `single identical points intersect`() {
            val hullA = listOf(Point(5f, 5f))
            val hullB = listOf(Point(5f, 5f))

            val intersects = intersects(hullA, hullB)

            assertThat(intersects).isTrue()
        }

        @Test
        fun `single different points do not intersect`() {
            val hullA = listOf(Point(0f, 0f))
            val hullB = listOf(Point(10f, 10f))

            val intersects = intersects(hullA, hullB)

            assertThat(intersects).isFalse()
        }

        @Test
        fun `non-overlapping diagonal squares with overlapping bounding boxes`() {
            // These squares' bounding boxes overlap but the actual shapes don't
            val hullA =
                listOf(
                    Point(0f, 5f),
                    Point(5f, 0f),
                    Point(10f, 5f),
                    Point(5f, 10f),
                )
            val hullB =
                listOf(
                    Point(8f, 13f),
                    Point(13f, 8f),
                    Point(18f, 13f),
                    Point(13f, 18f),
                )

            val intersects = intersects(hullA, hullB)

            assertThat(intersects).isFalse()
        }
    }
}
