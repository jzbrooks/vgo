@file:OptIn(ExperimentalVgoApi::class)

package com.jzbrooks.vgo

import com.jzbrooks.BuildConstants
import com.jzbrooks.vgo.core.Writer
import com.jzbrooks.vgo.core.util.ExperimentalVgoApi
import com.jzbrooks.vgo.iv.ImageVector
import com.jzbrooks.vgo.iv.ImageVectorOptimizationRegistry
import com.jzbrooks.vgo.iv.ImageVectorWriter
import com.jzbrooks.vgo.svg.ScalableVectorGraphic
import com.jzbrooks.vgo.svg.ScalableVectorGraphicWriter
import com.jzbrooks.vgo.svg.SvgOptimizationRegistry
import com.jzbrooks.vgo.svg.toVectorDrawable
import com.jzbrooks.vgo.util.parse
import com.jzbrooks.vgo.vd.VectorDrawable
import com.jzbrooks.vgo.vd.VectorDrawableOptimizationRegistry
import com.jzbrooks.vgo.vd.VectorDrawableWriter
import com.jzbrooks.vgo.vd.toImageVector
import com.jzbrooks.vgo.vd.toSvg
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.isSameFileAs
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString
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

        val inputOutputMap = pairOutputs()
        val files = inputOutputMap.count { (input, _) -> input.isFile }
        val containsDirectory = inputOutputMap.any { (input, _) -> input.isDirectory }
        printFileNames = options.printStats && (files > 1 || containsDirectory)

        return handleFiles(inputOutputMap, writerOptions)
    }

    private fun pairOutputs(): Map<File, Path> =
        if (options.output.isNotEmpty()) {
            options.input.zip(options.output) { a, b ->
                Pair(File(a), Paths.get(b))
            }
        } else {
            options.input.zip(options.input) { a, b ->
                Pair(File(a), Paths.get(b))
            }
        }.toMap()

    private fun handleFile(
        input: File,
        outputPath: Path,
        writerOptions: Set<Writer.Option>,
    ) {
        val output =
            if (input.path == outputPath.pathString) {
                when (options.format) {
                    "vd" -> outputPath.resolveSibling("${outputPath.nameWithoutExtension}.xml")
                    "svg" -> outputPath.resolveSibling("${outputPath.nameWithoutExtension}.svg")
                    "iv" -> outputPath.resolveSibling("${outputPath.nameWithoutExtension}.kt")
                    else -> outputPath
                }
            } else {
                outputPath
            }.toFile()

        if (output.parentFile?.exists() == false) output.parentFile.mkdirs()
        if (!output.exists()) output.createNewFile()

        val sizeBefore = input.length()
        var graphic = parse(input, options.format)

        // When the inputs are directories, the non-vector files shouldn't be skipped.
        // If the corresponding output path differs, the file will be copied below if
        // it is unable to be parsed.
        if (graphic == null && outputPath.isSameFileAs(input.toPath())) return

        output.outputStream().use { outputStream ->
            if (graphic != null) {
                when (options.format) {
                    "vd" -> {
                        if (graphic is ScalableVectorGraphic) {
                            graphic = graphic.toVectorDrawable()
                        }
                    }
                    "svg" -> {
                        if (graphic is VectorDrawable) {
                            graphic = graphic.toSvg()
                        }
                    }
                    "iv" -> {
                        if (graphic is VectorDrawable) {
                            graphic = graphic.toImageVector()
                        }
                    }
                    else -> System.err.println("Unknown format ${options.format}")
                }

                if (!options.noOptimization) {
                    val optimizationRegistry =
                        when (graphic) {
                            is VectorDrawable -> VectorDrawableOptimizationRegistry()
                            is ScalableVectorGraphic -> SvgOptimizationRegistry()
                            is ImageVector -> ImageVectorOptimizationRegistry()
                            else -> null
                        }

                    optimizationRegistry?.apply(graphic)
                }

                if (graphic is VectorDrawable) {
                    val writer = VectorDrawableWriter(writerOptions)
                    writer.write(graphic, outputStream)
                }

                if (graphic is ScalableVectorGraphic) {
                    val writer = ScalableVectorGraphicWriter(writerOptions)
                    writer.write(graphic, outputStream)
                }

                if (graphic is ImageVector) {
                    val writer = ImageVectorWriter(writerOptions)
                    writer.write(graphic, outputStream)
                }

                if (options.printStats) {
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
            } else if (input != output) {
                input.inputStream().use { it.copyTo(outputStream) }
            }
        }
    }

    private fun handleFiles(
        inputOutputMap: Map<File, Path>,
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
                    val output = output.toFile()
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

    private fun handleDirectory(
        input: File,
        output: Path,
        options: Set<Writer.Option>,
    ) {
        assert(input.isDirectory)
        assert(output.isDirectory() || !output.exists())

        for (file in input.walkTopDown().filter { file -> !file.isHidden && !file.isDirectory }) {
            handleFile(file, output.resolve(file.name), options)
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

    private val Map.Entry<File, Path>.inputExists
        get() = key.exists()

    private val Map.Entry<File, Path>.isFilePair
        get() = key.isFile && (value.isRegularFile() || !value.exists())

    private val Map.Entry<File, Path>.isDirectoryPair
        get() = key.isDirectory && (value.isDirectory() || !value.exists())

    data class Options(
        val printVersion: Boolean = false,
        val printStats: Boolean = false,
        val output: List<String> = emptyList(),
        val input: List<String> = emptyList(),
        val indent: Int? = null,
        val format: String? = null,
        val noOptimization: Boolean = false,
    )
}
