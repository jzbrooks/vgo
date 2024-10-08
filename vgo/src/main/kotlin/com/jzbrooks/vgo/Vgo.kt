package com.jzbrooks.vgo

import com.android.ide.common.vectordrawable.Svg2Vector
import com.jzbrooks.BuildConstants
import com.jzbrooks.vgo.core.Writer
import com.jzbrooks.vgo.svg.ScalableVectorGraphic
import com.jzbrooks.vgo.svg.ScalableVectorGraphicWriter
import com.jzbrooks.vgo.svg.SvgOptimizationRegistry
import com.jzbrooks.vgo.svg.parse
import com.jzbrooks.vgo.util.xml.asSequence
import com.jzbrooks.vgo.vd.VectorDrawable
import com.jzbrooks.vgo.vd.VectorDrawableOptimizationRegistry
import com.jzbrooks.vgo.vd.VectorDrawableWriter
import com.jzbrooks.vgo.vd.toSvg
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class Vgo(
    private val options: Options,
) {
    private var printFileNames = false
    private var totalBytesBefore = 0.0
    private var totalBytesAfter = 0.0

    fun run(): Int {
        if (options.printVersion) {
            println(BuildConstants.VERSION_NAME)
            return 0
        }

        val writerOptions = mutableSetOf<Writer.Option>()
        options.indent?.let { indentColumns ->
            writerOptions.add(Writer.Option.Indent(indentColumns))
        }

        var inputs = options.input
        if (inputs.isEmpty()) {
            require(options.output.isEmpty())

            var path = readlnOrNull()
            val standardInPaths = mutableListOf<String>()
            while (path != null) {
                standardInPaths.add(path)
                path = readlnOrNull()
            }

            inputs = standardInPaths
        }

        val inputOutputMap =
            if (options.output.isNotEmpty()) {
                inputs.zip(options.output) { a, b ->
                    Pair(File(a), File(b))
                }
            } else {
                inputs.zip(inputs) { a, b ->
                    Pair(File(a), File(b))
                }
            }.toMap()

        val files = inputOutputMap.count { (input, _) -> input.isFile }
        val containsDirectory = inputOutputMap.any { (input, _) -> input.isDirectory }
        printFileNames = options.printStats && (files > 1 || containsDirectory)

        return handleFiles(inputOutputMap, writerOptions)
    }

    private fun handleFiles(
        inputOutputMap: Map<File, File>,
        writerOptions: Set<Writer.Option>,
    ): Int {
        for (entry in inputOutputMap) {
            val (input, output) = entry

            when {
                entry.isFilePair -> handleFile(input, output, writerOptions)
                entry.isDirectoryPair -> handleDirectory(input, output, writerOptions)
                !entry.inputExists -> {
                    System.err.println("${input.path} does not exist.")
                    return 65
                }
                else -> {
                    System.err.println(
                        """
                        A given input and output pair (grouped positionally)
                        must be either files or directories.
                        Input is a ${if (input.isFile) "file" else "directory"}
                            path: ${input.absolutePath}
                            exists: ${input.exists()}
                            isWritable: ${input.canWrite()}
                        Output is a ${if (output.isFile) "file" else "directory"}
                            path: ${output.absolutePath}
                            exists: ${input.exists()}
                            isWritable: ${input.canWrite()}

                        Storage: ${output.usableSpace} / ${output.totalSpace} is usable.
                        """.trimIndent(),
                    )

                    return 65
                }
            }
        }

        return 0
    }

    private fun handleFile(
        input: File,
        output: File,
        options: Set<Writer.Option>,
    ) {
        input.inputStream().use { inputStream ->
            val sizeBefore = inputStream.channel.size()

            val documentBuilderFactory = DocumentBuilderFactory.newInstance()

            val document = documentBuilderFactory.newDocumentBuilder().parse(input)
            document.documentElement.normalize()

            val rootNodes =
                document.childNodes
                    .asSequence()
                    .filter { it.nodeType == Document.ELEMENT_NODE }
                    .toList()

            var graphic =
                when {
                    rootNodes.any { it.nodeName == "svg" || input.extension == "svg" } -> {
                        if (this.options.format == "vd") {
                            ByteArrayOutputStream().use { pipeOrigin ->
                                val errors = Svg2Vector.parseSvgToXml(input.toPath(), pipeOrigin)
                                if (errors != "") {
                                    System.err.println(
                                        """
                                        Skipping ${input.path}

                                          $errors
                                        """.trimIndent(),
                                    )
                                    null
                                } else {
                                    val pipeTerminal = ByteArrayInputStream(pipeOrigin.toByteArray())
                                    val convertedDocument =
                                        documentBuilderFactory.newDocumentBuilder().parse(pipeTerminal)
                                    convertedDocument.documentElement.normalize()

                                    val documentRoot =
                                        convertedDocument.childNodes.asSequence().first {
                                            it.nodeType == Document.ELEMENT_NODE
                                        }

                                    com.jzbrooks.vgo.vd
                                        .parse(documentRoot)
                                }
                            }
                        } else {
                            parse(rootNodes.first())
                        }
                    }
                    rootNodes.any { it.nodeName == "vector" && input.extension == "xml" } -> {
                        com.jzbrooks.vgo.vd
                            .parse(rootNodes.first())
                    }
                    else -> if (input == output) return else null
                }

            if (graphic is VectorDrawable && this.options.format == "svg") {
                graphic = graphic.toSvg()
            }

            val optimizationRegistry =
                when (graphic) {
                    is VectorDrawable -> VectorDrawableOptimizationRegistry()
                    is ScalableVectorGraphic -> SvgOptimizationRegistry()
                    else -> null
                }

            if (graphic != null) {
                optimizationRegistry?.apply(graphic)
            }

            if (output.parentFile?.exists() == false) output.parentFile.mkdirs()
            if (!output.exists()) output.createNewFile()

            output.outputStream().use { outputStream ->
                if (graphic is VectorDrawable) {
                    val writer = VectorDrawableWriter(options)
                    writer.write(graphic, outputStream)
                }

                if (graphic is ScalableVectorGraphic) {
                    val writer = ScalableVectorGraphicWriter(options)
                    writer.write(graphic, outputStream)
                }

                if (graphic == null && input != output) {
                    inputStream.copyTo(outputStream)
                }

                if (this.options.printStats) {
                    val sizeAfter = outputStream.channel.size()
                    val percentSaved = ((sizeBefore - sizeAfter) / sizeBefore.toDouble()) * 100
                    totalBytesBefore += sizeBefore
                    totalBytesAfter += sizeAfter

                    if (percentSaved.absoluteValue > 1e-3) {
                        if (printFileNames) println("\n${input.path}")
                        println("Size before: " + formatByteDescription(sizeBefore))
                        println("Size after: " + formatByteDescription(sizeAfter))
                        println("Percent saved: $percentSaved")
                    }
                }
            }
        }
    }

    private fun handleDirectory(
        input: File,
        output: File,
        options: Set<Writer.Option>,
    ) {
        assert(input.isDirectory)
        assert(output.isDirectory || !output.exists())

        for (file in input.walkTopDown().filter { file -> !file.isHidden && !file.isDirectory }) {
            handleFile(file, File(output, file.name), options)
        }

        if (this.options.printStats) {
            val message = "| Total bytes saved: ${(totalBytesBefore - totalBytesAfter).roundToInt()} |"
            val border = "-".repeat(message.length)
            println(
                """

                $border
                $message
                $border
                """.trimIndent(),
            )
        }
    }

    private fun formatByteDescription(bytes: Long): String =
        when {
            bytes >= 1024 * 1024 * 1024 -> {
                val gigabytes = bytes / (1024.0 * 1024.0 * 1024.0)
                "%.2f GiB".format(gigabytes)
            }
            bytes >= 1024 * 1024 -> {
                val megabytes = bytes / (1024.0 * 1024.0)
                "%.2f MiB".format(megabytes)
            }
            bytes >= 1024 -> {
                val kilobytes = bytes / 1024.0
                "%.2f KiB".format(kilobytes)
            }
            else -> "$bytes B"
        }

    private val Map.Entry<File, File>.inputExists
        get() = key.exists()

    private val Map.Entry<File, File>.isFilePair
        get() = key.isFile && (value.isFile || !value.exists())

    private val Map.Entry<File, File>.isDirectoryPair
        get() = key.isDirectory && (value.isDirectory || !value.exists())

    data class Options(
        val printVersion: Boolean = false,
        val printStats: Boolean = false,
        val output: List<String> = emptyList(),
        val input: List<String> = emptyList(),
        val indent: Int? = null,
        val format: String? = null,
    )
}
