package com.jzbrooks.vgo.composable

import com.jzbrooks.vgo.core.Writer
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
        val composeColor = MemberName("androidx.compose.ui.graphics", "Color")
        val solidColorBrush = ClassName("androidx.compose.ui.graphics", "SolidColor")

        val codeBlock =
            CodeBlock
                .builder()
                .add(
                    "%T.Builder(defaultWidth = %L, defaultHeight = %L, viewportWidth = %L, viewportHeight = %L)\n",
                    imageVector,
                    graphic.foreign.getValue("defaultWidth") + ".dp",
                    graphic.foreign.getValue("defaultHeight") + ".dp",
                    graphic.foreign.getValue("viewportWidth"),
                    graphic.foreign.getValue("viewportHeight"),
                ).indent()

        codeBlock.withIndent {
            for (element in graphic.elements) {
                when (element) {
                    is Path -> {
                        codeBlock.add(".path(\n")
                        codeBlock.withIndent {
                            if (element.fill.alpha > 0u) {
                                codeBlock.add(
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
                                codeBlock.add(
                                    "stroke = %T(%M(%L, %L, %L, %L)),\n",
                                    solidColorBrush,
                                    composeColor,
                                    element.stroke.red,
                                    element.stroke.green,
                                    element.stroke.blue,
                                    element.stroke.alpha,
                                )
                                codeBlock.add("strokeLineWidth = %Lf\n", element.strokeWidth)
                            }
                        }
                        codeBlock.add(") {\n")
                        codeBlock.withIndent {
                            for (command in element.commands) {
                                when (command) {
                                    is MoveTo -> {
                                        val coord = command.parameters.first()
                                        if (command.variant == CommandVariant.ABSOLUTE) {
                                            codeBlock.add("moveTo(%Lf, %Lf)\n", coord.x, coord.y)
                                        } else {
                                            codeBlock.add("moveToRelative(%Lf, %Lf)\n", coord.x, coord.y)
                                        }

                                        for (parameter in command.parameters.drop(1)) {
                                            if (command.variant == CommandVariant.ABSOLUTE) {
                                                codeBlock.add("lineTo(%Lf, %Lf)\n", parameter.x, parameter.y)
                                            } else {
                                                codeBlock.add("lineToRelative(%Lf, %Lf)\n", parameter.x, parameter.y)
                                            }
                                        }
                                    }
                                    is LineTo -> {
                                        for (parameter in command.parameters) {
                                            if (command.variant == CommandVariant.ABSOLUTE) {
                                                codeBlock.add("lineTo(%Lf, %Lf)\n", parameter.x, parameter.y)
                                            } else {
                                                codeBlock.add("lineToRelative(%Lf, %Lf)\n", parameter.x, parameter.y)
                                            }
                                        }
                                    }
                                    is HorizontalLineTo -> {
                                        for (parameter in command.parameters) {
                                            if (command.variant == CommandVariant.ABSOLUTE) {
                                                codeBlock.add("horizontalLineTo(%Lf)\n", parameter)
                                            } else {
                                                codeBlock.add("horizontalLineToRelative(%Lf)\n", parameter)
                                            }
                                        }
                                    }
                                    is VerticalLineTo -> {
                                        for (parameter in command.parameters) {
                                            if (command.variant == CommandVariant.ABSOLUTE) {
                                                codeBlock.add("verticalLineTo(%Lf)\n", parameter)
                                            } else {
                                                codeBlock.add("verticalLineToRelative(%Lf)\n", parameter)
                                            }
                                        }
                                    }
                                    is CubicBezierCurve -> {
                                        for (parameter in command.parameters) {
                                            if (command.variant == CommandVariant.ABSOLUTE) {
                                                codeBlock.add(
                                                    "curveTo(%Lf, %Lf, %Lf, %Lf, %Lf, %Lf)\n",
                                                    parameter.startControl.x,
                                                    parameter.startControl.y,
                                                    parameter.endControl.x,
                                                    parameter.endControl.y,
                                                    parameter.end.x,
                                                    parameter.end.y,
                                                )
                                            } else {
                                                codeBlock.add(
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
                                                codeBlock.add(
                                                    "reflectiveCurveTo(%Lf, %Lf, %Lf, %Lf)\n",
                                                    parameter.endControl.x,
                                                    parameter.endControl.y,
                                                    parameter.end.x,
                                                    parameter.end.y,
                                                )
                                            } else {
                                                codeBlock.add(
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
                                                codeBlock.add(
                                                    "quadTo(%Lf, %Lf, %Lf, %Lf)\n",
                                                    parameter.control.x,
                                                    parameter.control.y,
                                                    parameter.end.x,
                                                    parameter.end.y,
                                                )
                                            } else {
                                                codeBlock.add(
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
                                                codeBlock.add(
                                                    "reflectiveQuadTo(%Lf, %Lf)\n",
                                                    parameter.x,
                                                    parameter.y,
                                                )
                                            } else {
                                                codeBlock.add(
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
                                                codeBlock.add(
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
                                                codeBlock.add(
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
                                        codeBlock.add("close()\n")
                                    }
                                }
                            }
                        }
                        codeBlock.add("}\n")
                    }
                }
            }
        }

        codeBlock.add(".build()")
        codeBlock.unindent()

        return PropertySpec
            .builder(graphic.id.toString(), imageVector)
            .initializer(codeBlock.build())
            .build()
    }
}
