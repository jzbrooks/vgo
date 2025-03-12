package com.jzbrooks.vgo.composable

import com.jzbrooks.vgo.core.Writer
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.vd.VectorDrawableCommandPrinter
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
        val vectorBuilder = ClassName("androidx.compose.ui.graphics.vector", "ImageVector.Builder")
        val composeColor = MemberName("androidx.compose.ui.graphics", "Color")
        val solidColorBrush = ClassName("androidx.compose.ui.graphics", "SolidColor")
        val addPathNodes = MemberName("androidx.compose.ui.graphics.vector", "addPathNodes")

        val codeBlock =
            CodeBlock
                .builder()
                .add(
                    "%T(defaultWidth = %L, defaultHeight = %L, viewportWidth = %L, viewportHeight = %L)\n",
                    vectorBuilder,
                    graphic.foreign.getValue("defaultWidth") + ".dp",
                    graphic.foreign.getValue("defaultHeight") + ".dp",
                    graphic.foreign.getValue("viewportWidth"),
                    graphic.foreign.getValue("viewportHeight"),
                ).indent()

        codeBlock.add(".apply {\n")
        codeBlock.withIndent {
            graphic.elements.filterIsInstance<Path>().forEach { path ->
                codeBlock.add("addPath(\n")
                codeBlock.withIndent {
                    codeBlock.add("pathData = %M(%S),\n", addPathNodes, convertPathCommandsToString(path))
                    codeBlock.add(
                        "fill = %T(%M(%L, %L, %L, %L)),\n",
                        solidColorBrush,
                        composeColor,
                        path.fill.red,
                        path.fill.green,
                        path.fill.blue,
                        path.fill.alpha,
                    )
                    codeBlock.add(
                        "stroke = %T(%M(%L, %L, %L, %L)),\n",
                        solidColorBrush,
                        composeColor,
                        path.stroke.red,
                        path.stroke.green,
                        path.stroke.blue,
                        path.stroke.alpha,
                    )
                    codeBlock.add("strokeLineWidth = %Lf\n", path.strokeWidth)
                }
                codeBlock.add(")\n")
            }
        }

        codeBlock.add("}\n")
        codeBlock.add(".build()")
        codeBlock.unindent()

        return PropertySpec
            .builder(graphic.id.toString(), imageVector)
            .initializer(codeBlock.build())
            .build()
    }

    private fun convertPathCommandsToString(path: Path): String {
        val printer = VectorDrawableCommandPrinter(3)
        return path.commands.joinToString("", transform = printer::print)
    }
}
