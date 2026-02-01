package com.jzbrooks.vgo.iv

import com.jzbrooks.vgo.core.Writer
import com.jzbrooks.vgo.core.graphic.ClipPath
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
import com.jzbrooks.vgo.core.util.ExperimentalVgoApi
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.withIndent
import java.io.OutputStream
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

@ExperimentalVgoApi
class ImageVectorWriter(
    private val fileName: String,
    override val options: Set<Writer.Option> = emptySet(),
    private val decimalFormat: DecimalFormat =
        DecimalFormat().apply {
            maximumFractionDigits = 2
            isDecimalSeparatorAlwaysShown = false
            isGroupingUsed = false
            roundingMode = RoundingMode.HALF_UP
            minimumIntegerDigits = 0
            decimalFormatSymbols = DecimalFormatSymbols(Locale.US)
        },
) : Writer<ImageVector> {
    override fun write(
        graphic: ImageVector,
        stream: OutputStream,
    ) {
        val fileSpec = graphic.toFileSpec(fileName, decimalFormat)
        write(fileSpec, stream)
    }

    fun write(
        fileSpec: FileSpec,
        stream: OutputStream,
    ) {
        stream.writer().use { writer ->
            fileSpec.writeTo(writer)
        }
    }
}

@ExperimentalVgoApi
fun ImageVector.toFileSpec(
    fileName: String,
    decimalFormat: DecimalFormat,
): FileSpec {
    val packageName = foreign[FOREIGN_KEY_PACKAGE_NAME] ?: ""
    val propertyName = foreign[FOREIGN_KEY_PROPERTY_NAME] ?: id ?: "vector"
    return FileSpec
        .builder(packageName, fileName)
        .addImport("androidx.compose.ui.unit", "dp")
        .addProperty(createImageVectorProperty(propertyName, decimalFormat))
        .addProperty(
            PropertySpec
                .builder(
                    "_$propertyName",
                    ClassName("androidx.compose.ui.graphics.vector", "ImageVector").copy(nullable = true),
                    KModifier.PRIVATE,
                ).initializer("null")
                .mutable(true)
                .build(),
        ).build()
}

@ExperimentalVgoApi
private fun ImageVector.createImageVectorProperty(
    propertyName: String,
    decimalFormat: DecimalFormat,
): PropertySpec {
    val imageVector = ClassName("androidx.compose.ui.graphics.vector", "ImageVector")

    val imageVectorAllocation =
        CodeBlock
            .builder()
            .add(
                "%T.Builder(defaultWidth = %L.dp, defaultHeight = %L.dp, viewportWidth = %Lf, viewportHeight = %Lf)\n",
                imageVector,
                decimalFormat.format(defaultWidthDp),
                decimalFormat.format(defaultHeightDp),
                decimalFormat.format(viewportWidth),
                decimalFormat.format(viewportHeight),
            )

    imageVectorAllocation.withIndent {
        for (element in elements) {
            imageVectorAllocation.add(".")
            emitElement(element, imageVectorAllocation, decimalFormat)
        }
    }

    imageVectorAllocation.add(".build()")

    val backingProperty = "_$propertyName"

    val lazyImageVectorAllocation =
        CodeBlock
            .builder()
            .add(
                """
                return $backingProperty ?: %L.also { $backingProperty = it }
                """.trimIndent(),
                imageVectorAllocation.build(),
            )

    return PropertySpec
        .builder(propertyName, imageVector)
        .getter(FunSpec.getterBuilder().addCode(lazyImageVectorAllocation.build()).build())
        .build()
}

private fun emitElement(
    element: Element,
    codeBlock: CodeBlock.Builder,
    decimalFormat: DecimalFormat,
) {
    val composeColor = MemberName("androidx.compose.ui.graphics", "Color")
    val solidColorBrush = ClassName("androidx.compose.ui.graphics", "SolidColor")

    when (element) {
        is Path -> {
            codeBlock.add("%M(\n", MemberName("androidx.compose.ui.graphics.vector", "path"))
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
                                add("moveTo(%Lf, %Lf)\n", decimalFormat.format(coord.x), coord.y)
                            } else {
                                add("moveToRelative(%Lf, %Lf)\n", decimalFormat.format(coord.x), coord.y)
                            }

                            for (parameter in command.parameters.drop(1)) {
                                if (command.variant == CommandVariant.ABSOLUTE) {
                                    add("lineTo(%Lf, %Lf)\n", decimalFormat.format(parameter.x), decimalFormat.format(parameter.y))
                                } else {
                                    add(
                                        "lineToRelative(%Lf, %Lf)\n",
                                        decimalFormat.format(parameter.x),
                                        decimalFormat.format(parameter.y),
                                    )
                                }
                            }
                        }
                        is LineTo -> {
                            for (parameter in command.parameters) {
                                if (command.variant == CommandVariant.ABSOLUTE) {
                                    add("lineTo(%Lf, %Lf)\n", decimalFormat.format(parameter.x), decimalFormat.format(parameter.y))
                                } else {
                                    add(
                                        "lineToRelative(%Lf, %Lf)\n",
                                        decimalFormat.format(parameter.x),
                                        decimalFormat.format(parameter.y),
                                    )
                                }
                            }
                        }
                        is HorizontalLineTo -> {
                            for (parameter in command.parameters) {
                                if (command.variant == CommandVariant.ABSOLUTE) {
                                    add("horizontalLineTo(%Lf)\n", decimalFormat.format(parameter))
                                } else {
                                    add("horizontalLineToRelative(%Lf)\n", decimalFormat.format(parameter))
                                }
                            }
                        }
                        is VerticalLineTo -> {
                            for (parameter in command.parameters) {
                                if (command.variant == CommandVariant.ABSOLUTE) {
                                    add("verticalLineTo(%Lf)\n", decimalFormat.format(parameter))
                                } else {
                                    add("verticalLineToRelative(%Lf)\n", decimalFormat.format(parameter))
                                }
                            }
                        }
                        is CubicBezierCurve -> {
                            for (parameter in command.parameters) {
                                if (command.variant == CommandVariant.ABSOLUTE) {
                                    add(
                                        "curveTo(%Lf, %Lf, %Lf, %Lf, %Lf, %Lf)\n",
                                        decimalFormat.format(parameter.startControl.x),
                                        decimalFormat.format(parameter.startControl.y),
                                        decimalFormat.format(parameter.endControl.x),
                                        decimalFormat.format(parameter.endControl.y),
                                        decimalFormat.format(parameter.end.x),
                                        decimalFormat.format(parameter.end.y),
                                    )
                                } else {
                                    add(
                                        "curveToRelative(%Lf, %Lf, %Lf, %Lf, %Lf, %Lf)\n",
                                        decimalFormat.format(parameter.startControl.x),
                                        decimalFormat.format(parameter.startControl.y),
                                        decimalFormat.format(parameter.endControl.x),
                                        decimalFormat.format(parameter.endControl.y),
                                        decimalFormat.format(parameter.end.x),
                                        decimalFormat.format(parameter.end.y),
                                    )
                                }
                            }
                        }
                        is SmoothCubicBezierCurve -> {
                            for (parameter in command.parameters) {
                                if (command.variant == CommandVariant.ABSOLUTE) {
                                    add(
                                        "reflectiveCurveTo(%Lf, %Lf, %Lf, %Lf)\n",
                                        decimalFormat.format(parameter.endControl.x),
                                        decimalFormat.format(parameter.endControl.y),
                                        decimalFormat.format(parameter.end.x),
                                        decimalFormat.format(parameter.end.y),
                                    )
                                } else {
                                    add(
                                        "reflectiveCurveToRelative(%Lf, %Lf, %Lf, %Lf)\n",
                                        decimalFormat.format(parameter.endControl.x),
                                        decimalFormat.format(parameter.endControl.y),
                                        decimalFormat.format(parameter.end.x),
                                        decimalFormat.format(parameter.end.y),
                                    )
                                }
                            }
                        }
                        is QuadraticBezierCurve -> {
                            for (parameter in command.parameters) {
                                if (command.variant == CommandVariant.ABSOLUTE) {
                                    add(
                                        "quadTo(%Lf, %Lf, %Lf, %Lf)\n",
                                        decimalFormat.format(parameter.control.x),
                                        decimalFormat.format(parameter.control.y),
                                        decimalFormat.format(parameter.end.x),
                                        decimalFormat.format(parameter.end.y),
                                    )
                                } else {
                                    add(
                                        "quadToRelative(%Lf, %Lf, %Lf, %Lf)\n",
                                        decimalFormat.format(parameter.control.x),
                                        decimalFormat.format(parameter.control.y),
                                        decimalFormat.format(parameter.end.x),
                                        decimalFormat.format(parameter.end.y),
                                    )
                                }
                            }
                        }
                        is SmoothQuadraticBezierCurve -> {
                            for (parameter in command.parameters) {
                                if (command.variant == CommandVariant.ABSOLUTE) {
                                    add(
                                        "reflectiveQuadTo(%Lf, %Lf)\n",
                                        decimalFormat.format(parameter.x),
                                        decimalFormat.format(parameter.y),
                                    )
                                } else {
                                    add(
                                        "reflectiveQuadToRelative(%Lf, %Lf)\n",
                                        decimalFormat.format(parameter.x),
                                        decimalFormat.format(parameter.y),
                                    )
                                }
                            }
                        }

                        is EllipticalArcCurve -> {
                            for (parameter in command.parameters) {
                                if (command.variant == CommandVariant.ABSOLUTE) {
                                    add(
                                        "arcTo(%Lf, %Lf, %Lf, %L, %L, %Lf, %Lf)\n",
                                        decimalFormat.format(parameter.radiusX),
                                        decimalFormat.format(parameter.radiusY),
                                        decimalFormat.format(parameter.angle),
                                        parameter.arc == EllipticalArcCurve.ArcFlag.LARGE,
                                        parameter.sweep == EllipticalArcCurve.SweepFlag.CLOCKWISE,
                                        decimalFormat.format(parameter.end.x),
                                        decimalFormat.format(parameter.end.y),
                                    )
                                } else {
                                    add(
                                        "arcToRelative(%Lf, %Lf, %Lf, %L, %L, %Lf, %Lf)\n",
                                        decimalFormat.format(parameter.radiusX),
                                        decimalFormat.format(parameter.radiusY),
                                        decimalFormat.format(parameter.angle),
                                        parameter.arc == EllipticalArcCurve.ArcFlag.LARGE,
                                        parameter.sweep == EllipticalArcCurve.SweepFlag.CLOCKWISE,
                                        decimalFormat.format(parameter.end.x),
                                        decimalFormat.format(parameter.end.y),
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

            codeBlock.add("%M(\n", MemberName("androidx.compose.ui.graphics.vector", "group"))
            codeBlock.withIndent {
                add("rotate = %Lf,\n", decimalFormat.format(rotation))
                add("scaleX = %Lf,\n", decimalFormat.format(scaleX))
                add("scaleY = %Lf,\n", decimalFormat.format(scaleY))
                add("translationX = %Lf,\n", decimalFormat.format(translationX))
                add("translationY = %Lf,\n", decimalFormat.format(translationY))
            }
            codeBlock.add(") {\n")
            codeBlock.withIndent {
                for (child in element.elements) {
                    emitElement(child, codeBlock, decimalFormat)
                }
            }
            codeBlock.add("}\n")
        }
        is ClipPath -> {
            val pathNode = ClassName("androidx.compose.ui.graphics.vector", "PathNode")

            codeBlock.add("%M(\n", MemberName("androidx.compose.ui.graphics.vector", "group"))
            codeBlock.withIndent {
                add("clipPathData = listOf(\n")
                withIndent {
                    // todo: This might be wrong? Can these commands be flatmapped?
                    //  I think the answer is likely no, because merging paths this way
                    //  can alter the semantics of the paths (e.g. the big merge path bug involving
                    //  path intersections), but this bit of functionality is only very badly supported
                    //  right now anyways.
                    for (command in element.elements.filterIsInstance<Path>().flatMap { it.commands }) {
                        when (command) {
                            is MoveTo -> {
                                val coord = command.parameters.first()
                                if (command.variant == CommandVariant.ABSOLUTE) {
                                    add("%T.MoveTo(%Lf, %Lf),\n", pathNode, decimalFormat.format(coord.x), coord.y)
                                } else {
                                    add("%T.RelativeMoveTo(%Lf, %Lf),\n", pathNode, decimalFormat.format(coord.x), coord.y)
                                }

                                for (parameter in command.parameters.drop(1)) {
                                    if (command.variant == CommandVariant.ABSOLUTE) {
                                        add(
                                            "%T.LineTo(%Lf, %Lf),\n",
                                            pathNode,
                                            decimalFormat.format(parameter.x),
                                            decimalFormat.format(parameter.y),
                                        )
                                    } else {
                                        add(
                                            "%T.RelativeLineTo(%Lf, %Lf),\n",
                                            pathNode,
                                            decimalFormat.format(parameter.x),
                                            decimalFormat.format(parameter.y),
                                        )
                                    }
                                }
                            }
                            is LineTo -> {
                                for (parameter in command.parameters) {
                                    if (command.variant == CommandVariant.ABSOLUTE) {
                                        add(
                                            "%T.LineTo(%Lf, %Lf),\n",
                                            pathNode,
                                            decimalFormat.format(parameter.x),
                                            decimalFormat.format(parameter.y),
                                        )
                                    } else {
                                        add(
                                            "%T.RelativeLineTo(%Lf, %Lf),\n",
                                            pathNode,
                                            decimalFormat.format(parameter.x),
                                            decimalFormat.format(parameter.y),
                                        )
                                    }
                                }
                            }
                            is HorizontalLineTo -> {
                                for (parameter in command.parameters) {
                                    if (command.variant == CommandVariant.ABSOLUTE) {
                                        add("%T.HorizontalLineTo(%Lf),\n", pathNode, decimalFormat.format(parameter))
                                    } else {
                                        add("%T.RelativeHorizontalLineTo(%Lf),\n", pathNode, decimalFormat.format(parameter))
                                    }
                                }
                            }
                            is VerticalLineTo -> {
                                for (parameter in command.parameters) {
                                    if (command.variant == CommandVariant.ABSOLUTE) {
                                        add("%T.VerticalLineTo(%Lf),\n", pathNode, decimalFormat.format(parameter))
                                    } else {
                                        add("%T.RelativeVerticalLineTo(%Lf),\n", pathNode, decimalFormat.format(parameter))
                                    }
                                }
                            }
                            is CubicBezierCurve -> {
                                for (parameter in command.parameters) {
                                    if (command.variant == CommandVariant.ABSOLUTE) {
                                        add(
                                            "%T.CurveTo(%Lf, %Lf, %Lf, %Lf, %Lf, %Lf),\n",
                                            pathNode,
                                            decimalFormat.format(parameter.startControl.x),
                                            decimalFormat.format(parameter.startControl.y),
                                            decimalFormat.format(parameter.endControl.x),
                                            decimalFormat.format(parameter.endControl.y),
                                            decimalFormat.format(parameter.end.x),
                                            decimalFormat.format(parameter.end.y),
                                        )
                                    } else {
                                        add(
                                            "%T.RelativeCurveTo(%Lf, %Lf, %Lf, %Lf, %Lf, %Lf),\n",
                                            pathNode,
                                            decimalFormat.format(parameter.startControl.x),
                                            decimalFormat.format(parameter.startControl.y),
                                            decimalFormat.format(parameter.endControl.x),
                                            decimalFormat.format(parameter.endControl.y),
                                            decimalFormat.format(parameter.end.x),
                                            decimalFormat.format(parameter.end.y),
                                        )
                                    }
                                }
                            }
                            is SmoothCubicBezierCurve -> {
                                for (parameter in command.parameters) {
                                    if (command.variant == CommandVariant.ABSOLUTE) {
                                        add(
                                            "%T.ReflectiveCurveTo(%Lf, %Lf, %Lf, %Lf),\n",
                                            pathNode,
                                            decimalFormat.format(parameter.endControl.x),
                                            decimalFormat.format(parameter.endControl.y),
                                            decimalFormat.format(parameter.end.x),
                                            decimalFormat.format(parameter.end.y),
                                        )
                                    } else {
                                        add(
                                            "%T.RelativeReflectiveCurveTo(%Lf, %Lf, %Lf, %Lf),\n",
                                            pathNode,
                                            decimalFormat.format(parameter.endControl.x),
                                            decimalFormat.format(parameter.endControl.y),
                                            decimalFormat.format(parameter.end.x),
                                            decimalFormat.format(parameter.end.y),
                                        )
                                    }
                                }
                            }
                            is QuadraticBezierCurve -> {
                                for (parameter in command.parameters) {
                                    if (command.variant == CommandVariant.ABSOLUTE) {
                                        add(
                                            "%T.QuadTo(%Lf, %Lf, %Lf, %Lf),\n",
                                            pathNode,
                                            decimalFormat.format(parameter.control.x),
                                            decimalFormat.format(parameter.control.y),
                                            decimalFormat.format(parameter.end.x),
                                            decimalFormat.format(parameter.end.y),
                                        )
                                    } else {
                                        add(
                                            "%T.RelativeQuadTo(%Lf, %Lf, %Lf, %Lf),\n",
                                            pathNode,
                                            decimalFormat.format(parameter.control.x),
                                            decimalFormat.format(parameter.control.y),
                                            decimalFormat.format(parameter.end.x),
                                            decimalFormat.format(parameter.end.y),
                                        )
                                    }
                                }
                            }
                            is SmoothQuadraticBezierCurve -> {
                                for (parameter in command.parameters) {
                                    if (command.variant == CommandVariant.ABSOLUTE) {
                                        add(
                                            "%T.ReflectiveQuadTo(%Lf, %Lf),\n",
                                            pathNode,
                                            decimalFormat.format(parameter.x),
                                            decimalFormat.format(parameter.y),
                                        )
                                    } else {
                                        add(
                                            "%T.RelativeReflectiveQuadTo(%Lf, %Lf),\n",
                                            pathNode,
                                            decimalFormat.format(parameter.x),
                                            decimalFormat.format(parameter.y),
                                        )
                                    }
                                }
                            }

                            is EllipticalArcCurve -> {
                                for (parameter in command.parameters) {
                                    if (command.variant == CommandVariant.ABSOLUTE) {
                                        add(
                                            "%T.ArcTo(%Lf, %Lf, %Lf, %L, %L, %Lf, %Lf),\n",
                                            pathNode,
                                            parameter.radiusX,
                                            parameter.radiusY,
                                            parameter.angle,
                                            parameter.arc == EllipticalArcCurve.ArcFlag.LARGE,
                                            parameter.sweep == EllipticalArcCurve.SweepFlag.CLOCKWISE,
                                            decimalFormat.format(parameter.end.x),
                                            decimalFormat.format(parameter.end.y),
                                        )
                                    } else {
                                        add(
                                            "%T.RelativeArcTo(%Lf, %Lf, %Lf, %L, %L, %Lf, %Lf),\n",
                                            parameter.radiusX,
                                            parameter.radiusY,
                                            parameter.angle,
                                            parameter.arc == EllipticalArcCurve.ArcFlag.LARGE,
                                            parameter.sweep == EllipticalArcCurve.SweepFlag.CLOCKWISE,
                                            decimalFormat.format(parameter.end.x),
                                            decimalFormat.format(parameter.end.y),
                                        )
                                    }
                                }
                            }

                            ClosePath -> {
                                add("%T.Close,\n", pathNode)
                            }
                        }
                    }
                }
                codeBlock.add("),\n")
            }
            codeBlock.add(") {\n")
            codeBlock.withIndent {
                for (child in element.elements) {
                    // todo: figure out how VGO tracks clip paths (the rules are slightly weird for VDs and I vaguely remember modeling being weird)
                    emitElement(child, codeBlock, decimalFormat)
                }
            }
            codeBlock.add("}\n")
        }
    }
}
