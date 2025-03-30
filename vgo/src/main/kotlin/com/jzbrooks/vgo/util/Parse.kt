package com.jzbrooks.vgo.util

import com.android.ide.common.vectordrawable.Svg2Vector
import com.jzbrooks.vgo.core.graphic.Graphic
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import com.jzbrooks.vgo.iv.parse as composableParse
import com.jzbrooks.vgo.svg.parse as svgParse
import com.jzbrooks.vgo.vd.parse as vdParse

fun parse(file: File): Graphic? = parse(file, null)

internal fun parse(
    file: File,
    format: String? = null,
): Graphic? {
    if (file.length() == 0L) return null

    return if (file.extension == "kt") {
        composableParse(file)
    } else {
        file.inputStream().use { inputStream ->
            val documentBuilderFactory = DocumentBuilderFactory.newInstance()
            val document = documentBuilderFactory.newDocumentBuilder().parse(inputStream)
            document.documentElement.normalize()

            val graphic =
                when {
                    document.documentElement.nodeName == "svg" || file.extension == "svg" -> {
                        if (format == "vd") {
                            ByteArrayOutputStream().use { pipeOrigin ->
                                val errors = Svg2Vector.parseSvgToXml(file.toPath(), pipeOrigin)
                                if (errors != "") {
                                    System.err.println(
                                        """
                                        Skipping ${file.path}
                                            $errors
                                        """.trimIndent(),
                                    )
                                    null
                                } else {
                                    val pipeTerminal = ByteArrayInputStream(pipeOrigin.toByteArray())
                                    val convertedDocument =
                                        documentBuilderFactory.newDocumentBuilder().parse(pipeTerminal)
                                    convertedDocument.documentElement.normalize()

                                    vdParse(convertedDocument.documentElement)
                                }
                            }
                        } else {
                            svgParse(document.documentElement)
                        }
                    }

                    document.documentElement.nodeName == "vector" && file.extension == "xml" -> vdParse(document.documentElement)
                    else -> null
                }

            graphic
        }
    }
}
