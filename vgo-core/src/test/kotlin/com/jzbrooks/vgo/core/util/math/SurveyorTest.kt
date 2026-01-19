package com.jzbrooks.vgo.core.util.math

import assertk.all
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import assertk.assertions.prop
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.QuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothCubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothQuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo
import org.junit.jupiter.api.Test

class SurveyorTest {
    @Test
    fun `bounding boxes overlap but shapes do not intersect`() {
        // Two right triangles positioned diagonally:
        // Shape 1: bottom-left triangle (0,0) -> (100,0) -> (0,100)
        //   - hypotenuse lies on line x + y = 100
        // Shape 2: top-right triangle (100,100) -> (100,50) -> (50,100)
        //   - hypotenuse lies on line x + y = 150
        // Bounding boxes overlap in region (50,50) to (100,100)
        // but the actual triangles don't touch

        val commands1 =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 0f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 100f))),
            )

        val commands2 =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 100f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 50f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(50f, 100f))),
            )

        val surveyor = Surveyor()

        val box1 = surveyor.findBoundingBox(commands1)
        val box2 = surveyor.findBoundingBox(commands2)

        assertThat(box1.intersects(box2)).isTrue()

        val hull1 = surveyor.sampleConvexHull(commands1)
        val hull2 = surveyor.sampleConvexHull(commands2)

        assertThat(intersects(hull1, hull2)).isFalse()
    }

    @Test
    fun `line bounding box`() {
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
            prop(Rectangle::left).isCloseTo(10f, 0.1f)
            prop(Rectangle::top).isCloseTo(20f, 0.1f)
            prop(Rectangle::right).isCloseTo(40f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(5f, 0.1f)
        }
    }

    @Test
    fun `cubic curve bounding box`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(
                        CubicBezierCurve.Parameter(
                            Point(0f, 50f),
                            Point(10f, 50f),
                            Point(20f, 100f),
                        ),
                    ),
                ),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(5.9f, 0.1f)
            prop(Rectangle::top).isCloseTo(100f, 0.1f)
            prop(Rectangle::right).isCloseTo(20f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(10f, 0.1f)
        }
    }

    @Test
    fun `relative polycubic bounding box`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                CubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        CubicBezierCurve.Parameter(
                            Point(0f, 50f),
                            Point(10f, 50f),
                            Point(20f, 100f),
                        ),
                        CubicBezierCurve.Parameter(
                            Point(0f, 50f),
                            Point(10f, 50f),
                            Point(20f, 100f),
                        ),
                        CubicBezierCurve.Parameter(
                            Point(0f, 50f),
                            Point(10f, 50f),
                            Point(20f, 100f),
                        ),
                    ),
                ),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(10f, 0.1f)
            prop(Rectangle::top).isCloseTo(310f, 0.1f)
            prop(Rectangle::right).isCloseTo(70f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(10f, 0.1f)
        }
    }

    @Test
    fun `absolute polycubic bounding box`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(
                        CubicBezierCurve.Parameter(
                            Point(0f, 50f),
                            Point(10f, 50f),
                            Point(20f, 100f),
                        ),
                        CubicBezierCurve.Parameter(
                            Point(40f, 90f),
                            Point(70f, 120f),
                            Point(150f, 200f),
                        ),
                        CubicBezierCurve.Parameter(
                            Point(150f, 150f),
                            Point(175f, 175f),
                            Point(200f, 250f),
                        ),
                    ),
                ),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(5.9f, 0.1f)
            prop(Rectangle::top).isCloseTo(250f, 0.1f)
            prop(Rectangle::right).isCloseTo(200f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(10f, 0.1f)
        }
    }

    @Test
    fun `relative smooth polycubic bounding box`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                CubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        CubicBezierCurve.Parameter(
                            Point(0f, 50f),
                            Point(10f, 50f),
                            Point(20f, 100f),
                        ),
                    ),
                ),
                SmoothCubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        SmoothCubicBezierCurve.Parameter(
                            Point(10f, 50f),
                            Point(20f, 100f),
                        ),
                        SmoothCubicBezierCurve.Parameter(
                            Point(10f, 50f),
                            Point(20f, 100f),
                        ),
                    ),
                ),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(10f, 0.1f)
            prop(Rectangle::top).isCloseTo(310f, 0.1f)
            prop(Rectangle::right).isCloseTo(70f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(10f, 0.1f)
        }
    }

    @Test
    fun `absolute smooth polycubic bounding box`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(
                        CubicBezierCurve.Parameter(
                            Point(0f, 50f),
                            Point(10f, 50f),
                            Point(20f, 100f),
                        ),
                    ),
                ),
                SmoothCubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(
                        SmoothCubicBezierCurve.Parameter(
                            Point(70f, 120f),
                            Point(150f, 200f),
                        ),
                        SmoothCubicBezierCurve.Parameter(
                            Point(175f, 175f),
                            Point(200f, 250f),
                        ),
                    ),
                ),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(5.9f, 0.1f)
            prop(Rectangle::top).isCloseTo(250f, 0.1f)
            prop(Rectangle::right).isCloseTo(200f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(10f, 0.1f)
        }
    }

    @Test
    fun `smooth cubic bounding box`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(
                        CubicBezierCurve.Parameter(
                            Point(0f, 50f),
                            Point(10f, 50f),
                            Point(20f, 100f),
                        ),
                    ),
                ),
                SmoothCubicBezierCurve(CommandVariant.ABSOLUTE, listOf(SmoothCubicBezierCurve.Parameter(Point(0f, 25f), Point(10f, 75f)))),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(5.9f, 0.1f)
            prop(Rectangle::top).isCloseTo(111.8f, 0.1f)
            prop(Rectangle::right).isCloseTo(21.9f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(10f, 0.1f)
        }
    }

    @Test
    fun `quadratic curve bounding box`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                QuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(QuadraticBezierCurve.Parameter(Point(40f, 10f), Point(10f, 40f)))),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(10f, 0.1f)
            prop(Rectangle::top).isCloseTo(40f, 0.1f)
            prop(Rectangle::right).isCloseTo(25f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(10f, 0.1f)
        }
    }

    @Test
    fun `absolute polyquadratic curve bounding box`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                QuadraticBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(
                        QuadraticBezierCurve.Parameter(
                            Point(40f, 10f),
                            Point(10f, 40f),
                        ),
                        QuadraticBezierCurve.Parameter(
                            Point(90f, 40f),
                            Point(50f, 80f),
                        ),
                    ),
                ),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(10f, 0.1f)
            prop(Rectangle::top).isCloseTo(80f, 0.1f)
            prop(Rectangle::right).isCloseTo(63.2f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(10f, 0.1f)
        }
    }

    @Test
    fun `relative polyquadratic curve bounding box`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                QuadraticBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        QuadraticBezierCurve.Parameter(
                            Point(40f, 10f),
                            Point(10f, 40f),
                        ),
                        QuadraticBezierCurve.Parameter(
                            Point(90f, 40f),
                            Point(50f, 80f),
                        ),
                    ),
                ),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(10f, 0.1f)
            prop(Rectangle::top).isCloseTo(130f, 0.1f)
            prop(Rectangle::right).isCloseTo(82.3f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(10f, 0.1f)
        }
    }

    @Test
    fun `absolute smooth polyquadratic curve bounding box`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                QuadraticBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(
                        QuadraticBezierCurve.Parameter(
                            Point(40f, 10f),
                            Point(10f, 40f),
                        ),
                    ),
                ),
                SmoothQuadraticBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(
                        Point(90f, 40f),
                        Point(50f, 80f),
                    ),
                ),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(3.6f, 0.1f)
            prop(Rectangle::top).isCloseTo(80f, 0.1f)
            prop(Rectangle::right).isCloseTo(136.4f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(10f, 0.1f)
        }
    }

    @Test
    fun `relative smooth polyquadratic curve bounding box`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                QuadraticBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(
                        QuadraticBezierCurve.Parameter(
                            Point(40f, 10f),
                            Point(10f, 40f),
                        ),
                    ),
                ),
                SmoothQuadraticBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        Point(90f, 40f),
                        Point(50f, 80f),
                    ),
                ),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(4f, 0.1f)
            prop(Rectangle::top).isCloseTo(160f, 0.1f)
            prop(Rectangle::right).isCloseTo(175.6f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(10f, 0.1f)
        }
    }

    @Test
    fun `absolute elliptical arc curve bounding box`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(200f, 200f))),
                EllipticalArcCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(
                        EllipticalArcCurve.Parameter(
                            50f,
                            50f,
                            0f,
                            EllipticalArcCurve.ArcFlag.SMALL,
                            EllipticalArcCurve.SweepFlag.CLOCKWISE,
                            Point(200f, 100f),
                        ),
                    ),
                ),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(150f, 0.1f)
            prop(Rectangle::top).isCloseTo(200f, 0.1f)
            prop(Rectangle::right).isCloseTo(250f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(100f, 0.1f)
        }
    }

    @Test
    fun `relative elliptical arc curve bounding box`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(200f, 200f))),
                EllipticalArcCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        EllipticalArcCurve.Parameter(
                            50f,
                            50f,
                            0f,
                            EllipticalArcCurve.ArcFlag.SMALL,
                            EllipticalArcCurve.SweepFlag.CLOCKWISE,
                            Point(200f, 100f),
                        ),
                    ),
                ),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(188.1f, 0.1f)
            prop(Rectangle::top).isCloseTo(361.8f, 0.1f)
            prop(Rectangle::right).isCloseTo(411.8f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(138.2f, 0.1f)
        }
    }

    @Test
    fun `rotated elliptical arc curve bounding box`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(200f, 200f))),
                EllipticalArcCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(
                        EllipticalArcCurve.Parameter(
                            50f,
                            80f,
                            40f,
                            EllipticalArcCurve.ArcFlag.SMALL,
                            EllipticalArcCurve.SweepFlag.CLOCKWISE,
                            Point(400f, 400f),
                        ),
                    ),
                ),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(119.1f, 0.1f)
            prop(Rectangle::top).isCloseTo(495.3f, 0.1f)
            prop(Rectangle::right).isCloseTo(481f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(104.7f, 0.1f)
        }
    }

    @Test
    fun `moveto polycommand bounding box`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.RELATIVE, listOf(Point(10f, 10f), Point(10f, 10f), Point(10f, 10f))),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(10f, 0.1f)
            prop(Rectangle::top).isCloseTo(30f, 0.1f)
            prop(Rectangle::right).isCloseTo(30f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(10f, 0.1f)
        }
    }

    @Test
    fun `shorthand after moveto bounding box`() {
        // M 100,200 S 200,300,300,200
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 200f))),
                SmoothCubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(SmoothCubicBezierCurve.Parameter(Point(200f, 300f), Point(300f, 200f))),
                ),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(100f, 0.1f)
            prop(Rectangle::top).isCloseTo(244.1f, 0.1f)
            prop(Rectangle::right).isCloseTo(300f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(200f, 0.1f)
        }
    }

    @Test
    fun `heart bounding box`() {
        // M 10,30
        // A 20,20 0,0,1 50,30
        // A 20,20 0,0,1 90,30
        // Q 90,60 50,90
        // Q 10,60 10,30
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 30f))),
                EllipticalArcCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(
                        EllipticalArcCurve.Parameter(
                            20f,
                            20f,
                            0f,
                            EllipticalArcCurve.ArcFlag.SMALL,
                            EllipticalArcCurve.SweepFlag.CLOCKWISE,
                            Point(50f, 30f),
                        ),
                    ),
                ),
                EllipticalArcCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(
                        EllipticalArcCurve.Parameter(
                            20f,
                            20f,
                            0f,
                            EllipticalArcCurve.ArcFlag.SMALL,
                            EllipticalArcCurve.SweepFlag.CLOCKWISE,
                            Point(90f, 30f),
                        ),
                    ),
                ),
                QuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(QuadraticBezierCurve.Parameter(Point(90f, 60f), Point(50f, 90f)))),
                QuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(QuadraticBezierCurve.Parameter(Point(10f, 60f), Point(10f, 30f)))),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(10f, 0.1f)
            prop(Rectangle::top).isCloseTo(90f, 0.1f)
            prop(Rectangle::right).isCloseTo(90f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(10f, 0.1f)
        }
    }

    @Test
    fun `state bounding box`() {
        // M161.851,18.849C162.174,19.023 162.162,19.285 162.206,19.5C162.649,21.694 163.971,23.02 165.916,23.671C167.186,24.096 168.488,24.414 169.768,24.778C169.816,24.685 169.874,24.625 169.871,24.569C169.77,22.9 169.769,22.9 171.364,22.9L173.323,22.9C173.192,23.464 172.66,23.654 172.444,24.236C173.691,24.681 174.935,25.117 176.174,25.572C176.468,25.68 176.873,25.684 177.012,26.002C177.462,27.033 178.322,26.931 179.137,26.934C181.675,26.944 184.209,27.345 186.772,26.934C186.749,27.413 186.404,27.452 186.19,27.593C183.371,29.448 180.694,31.518 178.048,33.636C177.05,34.435 176.496,35.629 175.935,36.727C175.608,37.365 175.797,38.412 175.983,39.215C176.388,40.965 175.875,42.246 174.441,43.066C172.895,43.95 172.911,45.356 173.131,46.882C173.246,47.681 173.396,48.461 173.619,49.246C173.897,50.223 173.515,51.251 173.397,52.252C173.203,53.893 173.861,55.024 175.367,55.492C176.189,55.748 177.012,56.002 177.829,56.277C178.833,56.615 179.673,57.164 180.068,58.286C180.282,58.894 180.564,59.444 181.185,59.725C181.832,60.019 181.553,60.667 181.565,61.164C181.601,62.764 181.583,62.729 180.072,62.697C171.889,62.525 163.706,62.357 155.522,62.236C154.716,62.224 154.542,61.995 154.537,61.172C154.513,57.578 154.386,53.984 154.366,50.389C154.362,49.619 153.935,49.23 153.554,48.76C152.69,47.694 152.49,46.395 153.291,45.295C154.015,44.301 154.041,43.361 153.823,42.195C153.251,39.144 152.868,36.056 152.538,32.962C152.496,32.562 152.47,32.135 152.551,31.747C152.844,30.349 152.686,29.048 151.854,27.908C150.742,26.384 150.876,24.622 150.896,22.864C150.919,20.852 150.902,20.879 152.749,20.822C155.263,20.744 157.777,20.63 160.291,20.51C161.714,20.442 161.742,20.392 161.851,18.849
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(161.851f, 18.849f))),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(162.174f, 19.023f), Point(162.162f, 19.285f), Point(162.206f, 19.5f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(162.649f, 21.694f), Point(163.971f, 23.02f), Point(165.916f, 23.671f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(167.186f, 24.096f), Point(168.488f, 24.414f), Point(169.768f, 24.778f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(169.816f, 24.685f), Point(169.874f, 24.625f), Point(169.871f, 24.569f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(169.77f, 22.9f), Point(169.769f, 22.9f), Point(171.364f, 22.9f))),
                ),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(173.323f, 22.9f))),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(173.192f, 23.464f), Point(172.66f, 23.654f), Point(172.444f, 24.236f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(173.691f, 24.681f), Point(174.935f, 25.117f), Point(176.174f, 25.572f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(176.468f, 25.68f), Point(176.873f, 25.684f), Point(177.012f, 26.002f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(177.462f, 27.033f), Point(178.322f, 26.931f), Point(179.137f, 26.934f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(181.675f, 26.944f), Point(184.209f, 27.345f), Point(186.772f, 26.934f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(186.749f, 27.413f), Point(186.404f, 27.452f), Point(186.19f, 27.593f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(183.371f, 29.448f), Point(180.694f, 31.518f), Point(178.048f, 33.636f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(177.05f, 34.435f), Point(176.496f, 35.629f), Point(175.935f, 36.727f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(175.608f, 37.365f), Point(175.797f, 38.412f), Point(175.983f, 39.215f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(176.388f, 40.965f), Point(175.875f, 42.246f), Point(174.441f, 43.066f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(172.895f, 43.95f), Point(172.911f, 45.356f), Point(173.131f, 46.882f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(173.246f, 47.681f), Point(173.396f, 48.461f), Point(173.619f, 49.246f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(173.897f, 50.223f), Point(173.515f, 51.251f), Point(173.397f, 52.252f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(173.203f, 53.893f), Point(173.861f, 55.024f), Point(175.367f, 55.492f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(176.189f, 55.748f), Point(177.012f, 56.002f), Point(177.829f, 56.277f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(178.833f, 56.615f), Point(179.673f, 57.164f), Point(180.068f, 58.286f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(180.282f, 58.894f), Point(180.564f, 59.444f), Point(181.185f, 59.725f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(181.832f, 60.019f), Point(181.553f, 60.667f), Point(181.565f, 61.164f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(181.601f, 62.764f), Point(181.583f, 62.729f), Point(180.072f, 62.697f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(171.889f, 62.525f), Point(163.706f, 62.357f), Point(155.522f, 62.236f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(154.716f, 62.224f), Point(154.542f, 61.995f), Point(154.537f, 61.172f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(154.513f, 57.578f), Point(154.386f, 53.984f), Point(154.366f, 50.389f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(154.362f, 49.619f), Point(153.935f, 49.23f), Point(153.554f, 48.76f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(152.69f, 47.694f), Point(152.49f, 46.395f), Point(153.291f, 45.295f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(154.015f, 44.301f), Point(154.041f, 43.361f), Point(153.823f, 42.195f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(153.251f, 39.144f), Point(152.868f, 36.056f), Point(152.538f, 32.962f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(152.496f, 32.562f), Point(152.47f, 32.135f), Point(152.551f, 31.747f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(152.844f, 30.349f), Point(152.686f, 29.048f), Point(151.854f, 27.908f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(150.742f, 26.384f), Point(150.876f, 24.622f), Point(150.896f, 22.864f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(150.919f, 20.852f), Point(150.902f, 20.879f), Point(152.749f, 20.822f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(155.263f, 20.744f), Point(157.777f, 20.63f), Point(160.291f, 20.51f))),
                ),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(CubicBezierCurve.Parameter(Point(161.714f, 20.442f), Point(161.742f, 20.392f), Point(161.851f, 18.849f))),
                ),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(150.9f, 0.1f)
            prop(Rectangle::top).isCloseTo(62.7f, 0.1f)
            prop(Rectangle::right).isCloseTo(186.8f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(18.8f, 0.1f)
        }
    }
}
