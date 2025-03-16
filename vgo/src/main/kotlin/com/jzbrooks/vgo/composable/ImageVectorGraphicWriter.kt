package com.jzbrooks.vgo.composable

import com.jzbrooks.vgo.core.Writer
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.ClosePath
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
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.withIndent
import java.io.OutputStream
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

class ImageVectorGraphicWriter(
    override val options: Set<Writer.Option> = emptySet(),
) : Writer<ImageVectorGraphic> {
    override fun write(
        graphic: ImageVectorGraphic,
        stream: OutputStream,
    ) {
        val fileSpec =
            FileSpec
                .builder(
                    packageName = graphic.packageName ?: "",
                    fileName = "${graphic.propertyName}.kt",
                ).addImport("androidx.compose.ui.unit", "dp")
                .addProperty(createImageVectorProperty(graphic))
                .build()

        stream.writer().use { writer ->
            fileSpec.writeTo(writer)
        }
    }

    private fun createImageVectorProperty(graphic: ImageVectorGraphic): PropertySpec {
        val imageVector = ClassName("androidx.compose.ui.graphics.vector", "ImageVector")

        val codeBlock =
            CodeBlock
                .builder()
                .add(
                    "%T.Builder(defaultWidth = %L.dp, defaultHeight = %L.dp, viewportWidth = %Lf, viewportHeight = %Lf)\n",
                    imageVector,
                    graphic.foreign.getValue("defaultWidth"),
                    graphic.foreign.getValue("defaultHeight"),
                    graphic.foreign.getValue("viewportWidth"),
                    graphic.foreign.getValue("viewportHeight"),
                ).indent()

        codeBlock.withIndent {
            for (element in graphic.elements) {
                codeBlock.add(".")
                emitElement(element, codeBlock)
            }
        }

        codeBlock.add(".build()")
        codeBlock.unindent()

        return PropertySpec
            .builder(graphic.propertyName, imageVector)
            .initializer(codeBlock.build())
            .build()
    }

    private fun emitElement(
        element: Element,
        codeBlock: CodeBlock.Builder,
    ) {
        val composeColor = MemberName("androidx.compose.ui.graphics", "Color")
        val solidColorBrush = ClassName("androidx.compose.ui.graphics", "SolidColor")

        when (element) {
            is Path -> {
                codeBlock.add("path(\n")
                codeBlock.withIndent {
                    if (element.fill.alpha > 0u) {
                        add(
                            "fill = %T(%M(%L, %L, %L, %L)),\n",
                            solidColorBrush,
                            composeColor,
                            element.fill.red,
                            element.fill.green,
                            element.fill.blue,
                            element.fill.alpha,
                        )
                    }

                    if (element.stroke.alpha > 0u && element.strokeWidth > 0f) {
                        add(
                            "stroke = %T(%M(%L, %L, %L, %L)),\n",
                            solidColorBrush,
                            composeColor,
                            element.stroke.red,
                            element.stroke.green,
                            element.stroke.blue,
                            element.stroke.alpha,
                        )
                        add("strokeLineWidth = %Lf\n", element.strokeWidth)
                    }
                }
                codeBlock.add(") {\n")
                codeBlock.withIndent {
                    for (command in element.commands) {
                        when (command) {
                            is MoveTo -> {
                                val coord = command.parameters.first()
                                if (command.variant == CommandVariant.ABSOLUTE) {
                                    add("moveTo(%Lf, %Lf)\n", coord.x, coord.y)
                                } else {
                                    add("moveToRelative(%Lf, %Lf)\n", coord.x, coord.y)
                                }

                                for (parameter in command.parameters.drop(1)) {
                                    if (command.variant == CommandVariant.ABSOLUTE) {
                                        add("lineTo(%Lf, %Lf)\n", parameter.x, parameter.y)
                                    } else {
                                        add("lineToRelative(%Lf, %Lf)\n", parameter.x, parameter.y)
                                    }
                                }
                            }
                            is LineTo -> {
                                for (parameter in command.parameters) {
                                    if (command.variant == CommandVariant.ABSOLUTE) {
                                        add("lineTo(%Lf, %Lf)\n", parameter.x, parameter.y)
                                    } else {
                                        add("lineToRelative(%Lf, %Lf)\n", parameter.x, parameter.y)
                                    }
                                }
                            }
                            is HorizontalLineTo -> {
                                for (parameter in command.parameters) {
                                    if (command.variant == CommandVariant.ABSOLUTE) {
                                        add("horizontalLineTo(%Lf)\n", parameter)
                                    } else {
                                        add("horizontalLineToRelative(%Lf)\n", parameter)
                                    }
                                }
                            }
                            is VerticalLineTo -> {
                                for (parameter in command.parameters) {
                                    if (command.variant == CommandVariant.ABSOLUTE) {
                                        add("verticalLineTo(%Lf)\n", parameter)
                                    } else {
                                        add("verticalLineToRelative(%Lf)\n", parameter)
                                    }
                                }
                            }
                            is CubicBezierCurve -> {
                                for (parameter in command.parameters) {
                                    if (command.variant == CommandVariant.ABSOLUTE) {
                                        add(
                                            "curveTo(%Lf, %Lf, %Lf, %Lf, %Lf, %Lf)\n",
                                            parameter.startControl.x,
                                            parameter.startControl.y,
                                            parameter.endControl.x,
                                            parameter.endControl.y,
                                            parameter.end.x,
                                            parameter.end.y,
                                        )
                                    } else {
                                        add(
                                            "curveToRelative(%Lf, %Lf, %Lf, %Lf, %Lf, %Lf)\n",
                                            parameter.startControl.x,
                                            parameter.startControl.y,
                                            parameter.endControl.x,
                                            parameter.endControl.y,
                                            parameter.end.x,
                                            parameter.end.y,
                                        )
                                    }
                                }
                            }
                            is SmoothCubicBezierCurve -> {
                                for (parameter in command.parameters) {
                                    if (command.variant == CommandVariant.ABSOLUTE) {
                                        add(
                                            "reflectiveCurveTo(%Lf, %Lf, %Lf, %Lf)\n",
                                            parameter.endControl.x,
                                            parameter.endControl.y,
                                            parameter.end.x,
                                            parameter.end.y,
                                        )
                                    } else {
                                        add(
                                            "reflectiveCurveToRelative(%Lf, %Lf, %Lf, %Lf)\n",
                                            parameter.endControl.x,
                                            parameter.endControl.y,
                                            parameter.end.x,
                                            parameter.end.y,
                                        )
                                    }
                                }
                            }
                            is QuadraticBezierCurve -> {
                                for (parameter in command.parameters) {
                                    if (command.variant == CommandVariant.ABSOLUTE) {
                                        add(
                                            "quadTo(%Lf, %Lf, %Lf, %Lf)\n",
                                            parameter.control.x,
                                            parameter.control.y,
                                            parameter.end.x,
                                            parameter.end.y,
                                        )
                                    } else {
                                        add(
                                            "quadToRelative(%Lf, %Lf, %Lf, %Lf)\n",
                                            parameter.control.x,
                                            parameter.control.y,
                                            parameter.end.x,
                                            parameter.end.y,
                                        )
                                    }
                                }
                            }
                            is SmoothQuadraticBezierCurve -> {
                                for (parameter in command.parameters) {
                                    if (command.variant == CommandVariant.ABSOLUTE) {
                                        add(
                                            "reflectiveQuadTo(%Lf, %Lf)\n",
                                            parameter.x,
                                            parameter.y,
                                        )
                                    } else {
                                        add(
                                            "reflectiveQuadToRelative(%Lf, %Lf)\n",
                                            parameter.x,
                                            parameter.y,
                                        )
                                    }
                                }
                            }

                            is EllipticalArcCurve -> {
                                for (parameter in command.parameters) {
                                    if (command.variant == CommandVariant.ABSOLUTE) {
                                        add(
                                            "arcTo(%Lf, %Lf, %Lf, %L, %L, %Lf, %Lf)\n",
                                            parameter.radiusX,
                                            parameter.radiusY,
                                            parameter.angle,
                                            parameter.arc == EllipticalArcCurve.ArcFlag.LARGE,
                                            parameter.sweep == EllipticalArcCurve.SweepFlag.CLOCKWISE,
                                            parameter.end.x,
                                            parameter.end.y,
                                        )
                                    } else {
                                        add(
                                            "arcToRelative(%Lf, %Lf, %Lf, %L, %L, %Lf, %Lf)\n",
                                            parameter.radiusX,
                                            parameter.radiusY,
                                            parameter.angle,
                                            parameter.arc == EllipticalArcCurve.ArcFlag.LARGE,
                                            parameter.sweep == EllipticalArcCurve.SweepFlag.CLOCKWISE,
                                            parameter.end.x,
                                            parameter.end.y,
                                        )
                                    }
                                }
                            }

                            ClosePath -> {
                                add("close()\n")
                            }
                        }
                    }
                }
                codeBlock.add("}\n")
            }
            is Group -> {
                val matrix = element.transform

                val scaleX = hypot(matrix[0, 0], matrix[0, 1])
                val scaleY = hypot(matrix[1, 0], matrix[1, 1])

                val rotation = atan2(matrix[0, 1], matrix[0, 0]) * (180 / PI).toFloat()

                val translationX = matrix[0, 2]
                val translationY = matrix[1, 2]

                // todo: handle pivot

                codeBlock.add("group(\n")
                codeBlock.withIndent {
                    add("rotate = %Lf,\n", rotation)
                    add("scaleX = %Lf,\n", scaleX)
                    add("scaleY = %Lf,\n", scaleY)
                    add("translationX = %Lf,\n", translationX)
                    add("translationY = %Lf,\n", translationY)
                }
                codeBlock.add(") {\n")
                codeBlock.withIndent {
                    for (child in element.elements) {
                        emitElement(child, codeBlock)
                    }
                }
                codeBlock.add("}\n")
            }
        }
    }
}
