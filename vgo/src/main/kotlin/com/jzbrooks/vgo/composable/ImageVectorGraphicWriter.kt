package com.jzbrooks.vgo.composable

import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Writer
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.vd.VectorDrawableCommandPrinter
import com.squareup.kotlinpoet.*
import java.io.OutputStream

class ImageVectorGraphicWriter(
    override val options: Set<Writer.Option> = emptySet(),
) : Writer<ImageVectorGraphic> {
    override fun write(graphic: ImageVectorGraphic, stream: OutputStream) {
        val fileSpec = FileSpec.builder(
            packageName = graphic.packageName ?: "",
            fileName = "${graphic.propertyName}.kt",
        )
            .addProperty(createImageVectorProperty(graphic))
            .build()

        stream.writer().use { writer ->
            fileSpec.writeTo(writer)
        }
    }

    private fun createImageVectorProperty(graphic: ImageVectorGraphic): PropertySpec {
        val imageVector = ClassName("androidx.compose.ui.graphics.vector", "ImageVector")
        val vectorBuilder = ClassName("androidx.compose.ui.graphics.vector", "ImageVector.Builder")

        val codeBlock = CodeBlock.builder()
            .add("%M(%L, %L, %L, %L)\n",
                vectorBuilder,
                graphic.foreign.getValue("defaultWidth") + ".dp",
                graphic.foreign.getValue("defaultHeight") + ".dp",
                graphic.foreign.getValue("viewportWidth"),
                graphic.foreign.getValue("viewportHeight"))
            .indent()

        // Add paths from the graphic
        graphic.elements.filterIsInstance<Path>().forEach { path ->
            codeBlock.add(".addPath(\n")
            codeBlock.indent()
            codeBlock.add("pathData = %S,\n", convertPathCommandsToString(path))
            codeBlock.add("fill = %L,\n", path.fill.toHexString(Color.HexFormat.ARGB))
            codeBlock.add("stroke = %L,\n", path.stroke.toHexString(Color.HexFormat.ARGB))
            codeBlock.add("strokeLineWidth = %Lf\n", path.strokeWidth)
            codeBlock.unindent()
            codeBlock.add(")\n")
        }

        codeBlock.add(".build()")
        codeBlock.unindent()

        return PropertySpec.builder(graphic.id.toString(), imageVector)
            .initializer(codeBlock.build())
            .build()
    }

    private fun convertPathCommandsToString(path: Path): String {
        val printer = VectorDrawableCommandPrinter(3)
        return path.commands.joinToString("", transform = printer::print)
    }
}