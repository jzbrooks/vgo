package com.jzbrooks.vgo.core.transformation

import com.jzbrooks.vgo.core.graphic.Circle
import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Ellipse
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Line
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.Polygon
import com.jzbrooks.vgo.core.graphic.Polyline
import com.jzbrooks.vgo.core.graphic.Rect
import com.jzbrooks.vgo.core.graphic.Shape
import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo
import com.jzbrooks.vgo.core.util.math.Point

class ConvertShapesToPaths : TopDownTransformer {
    override fun visit(graphic: Graphic) = convertShapes(graphic)

    override fun visit(group: Group) = convertShapes(group)

    override fun visit(extra: Extra) {}

    override fun visit(shape: Shape) {}

    override fun visit(path: Path) {}

    private fun convertShapes(container: ContainerElement) {
        container.elements =
            container.elements.map { element ->
                if (element is Shape) convertToPath(element) else element
            }
    }

    companion object {
        fun convertToPath(shape: Shape): Path {
            val commands =
                when (shape) {
                    is Circle -> {
                        listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(shape.cx - shape.r, shape.cy))),
                            EllipticalArcCurve(
                                CommandVariant.ABSOLUTE,
                                listOf(
                                    EllipticalArcCurve.Parameter(
                                        shape.r,
                                        shape.r,
                                        0f,
                                        EllipticalArcCurve.ArcFlag.LARGE,
                                        EllipticalArcCurve.SweepFlag.CLOCKWISE,
                                        Point(shape.cx + shape.r, shape.cy),
                                    ),
                                ),
                            ),
                            EllipticalArcCurve(
                                CommandVariant.ABSOLUTE,
                                listOf(
                                    EllipticalArcCurve.Parameter(
                                        shape.r,
                                        shape.r,
                                        0f,
                                        EllipticalArcCurve.ArcFlag.LARGE,
                                        EllipticalArcCurve.SweepFlag.CLOCKWISE,
                                        Point(shape.cx - shape.r, shape.cy),
                                    ),
                                ),
                            ),
                            ClosePath,
                        )
                    }

                    is Ellipse -> {
                        listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(shape.cx - shape.rx, shape.cy))),
                            EllipticalArcCurve(
                                CommandVariant.ABSOLUTE,
                                listOf(
                                    EllipticalArcCurve.Parameter(
                                        shape.rx,
                                        shape.ry,
                                        0f,
                                        EllipticalArcCurve.ArcFlag.LARGE,
                                        EllipticalArcCurve.SweepFlag.CLOCKWISE,
                                        Point(shape.cx + shape.rx, shape.cy),
                                    ),
                                ),
                            ),
                            EllipticalArcCurve(
                                CommandVariant.ABSOLUTE,
                                listOf(
                                    EllipticalArcCurve.Parameter(
                                        shape.rx,
                                        shape.ry,
                                        0f,
                                        EllipticalArcCurve.ArcFlag.LARGE,
                                        EllipticalArcCurve.SweepFlag.CLOCKWISE,
                                        Point(shape.cx - shape.rx, shape.cy),
                                    ),
                                ),
                            ),
                            ClosePath,
                        )
                    }

                    is Rect -> {
                        if (shape.rx > 0f || shape.ry > 0f) {
                            listOf(
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(shape.x + shape.rx, shape.y))),
                                HorizontalLineTo(CommandVariant.ABSOLUTE, listOf(shape.x + shape.width - shape.rx)),
                                EllipticalArcCurve(
                                    CommandVariant.ABSOLUTE,
                                    listOf(
                                        EllipticalArcCurve.Parameter(
                                            shape.rx,
                                            shape.ry,
                                            0f,
                                            EllipticalArcCurve.ArcFlag.SMALL,
                                            EllipticalArcCurve.SweepFlag.CLOCKWISE,
                                            Point(shape.x + shape.width, shape.y + shape.ry),
                                        ),
                                    ),
                                ),
                                VerticalLineTo(CommandVariant.ABSOLUTE, listOf(shape.y + shape.height - shape.ry)),
                                EllipticalArcCurve(
                                    CommandVariant.ABSOLUTE,
                                    listOf(
                                        EllipticalArcCurve.Parameter(
                                            shape.rx,
                                            shape.ry,
                                            0f,
                                            EllipticalArcCurve.ArcFlag.SMALL,
                                            EllipticalArcCurve.SweepFlag.CLOCKWISE,
                                            Point(shape.x + shape.width - shape.rx, shape.y + shape.height),
                                        ),
                                    ),
                                ),
                                HorizontalLineTo(CommandVariant.ABSOLUTE, listOf(shape.x + shape.rx)),
                                EllipticalArcCurve(
                                    CommandVariant.ABSOLUTE,
                                    listOf(
                                        EllipticalArcCurve.Parameter(
                                            shape.rx,
                                            shape.ry,
                                            0f,
                                            EllipticalArcCurve.ArcFlag.SMALL,
                                            EllipticalArcCurve.SweepFlag.CLOCKWISE,
                                            Point(shape.x, shape.y + shape.height - shape.ry),
                                        ),
                                    ),
                                ),
                                VerticalLineTo(CommandVariant.ABSOLUTE, listOf(shape.y + shape.ry)),
                                EllipticalArcCurve(
                                    CommandVariant.ABSOLUTE,
                                    listOf(
                                        EllipticalArcCurve.Parameter(
                                            shape.rx,
                                            shape.ry,
                                            0f,
                                            EllipticalArcCurve.ArcFlag.SMALL,
                                            EllipticalArcCurve.SweepFlag.CLOCKWISE,
                                            Point(shape.x + shape.rx, shape.y),
                                        ),
                                    ),
                                ),
                                ClosePath,
                            )
                        } else {
                            listOf(
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(shape.x, shape.y))),
                                HorizontalLineTo(CommandVariant.ABSOLUTE, listOf(shape.x + shape.width)),
                                VerticalLineTo(CommandVariant.ABSOLUTE, listOf(shape.y + shape.height)),
                                HorizontalLineTo(CommandVariant.ABSOLUTE, listOf(shape.x)),
                                ClosePath,
                            )
                        }
                    }

                    is Line -> {
                        listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(shape.x1, shape.y1))),
                            LineTo(CommandVariant.ABSOLUTE, listOf(Point(shape.x2, shape.y2))),
                        )
                    }

                    is Polyline -> {
                        if (shape.points.isEmpty()) {
                            emptyList()
                        } else {
                            buildList {
                                add(MoveTo(CommandVariant.ABSOLUTE, listOf(shape.points.first())))
                                if (shape.points.size > 1) {
                                    add(LineTo(CommandVariant.ABSOLUTE, shape.points.drop(1)))
                                }
                            }
                        }
                    }

                    is Polygon -> {
                        if (shape.points.isEmpty()) {
                            emptyList()
                        } else {
                            buildList {
                                add(MoveTo(CommandVariant.ABSOLUTE, listOf(shape.points.first())))
                                if (shape.points.size > 1) {
                                    add(LineTo(CommandVariant.ABSOLUTE, shape.points.drop(1)))
                                }
                                add(ClosePath)
                            }
                        }
                    }
                }

            return Path(
                shape.id,
                shape.foreign,
                commands,
                shape.fill,
                shape.fillRule,
                shape.stroke,
                shape.strokeWidth,
                shape.strokeLineCap,
                shape.strokeLineJoin,
                shape.strokeMiterLimit,
            )
        }
    }
}
