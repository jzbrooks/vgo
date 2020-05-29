package com.jzbrooks.vgo

import com.jzbrooks.vgo.core.Writer
import com.jzbrooks.vgo.svg.ScalableVectorGraphic
import com.jzbrooks.vgo.svg.ScalableVectorGraphicWriter
import com.jzbrooks.vgo.svg.SvgOptimizationRegistry
import com.jzbrooks.vgo.svg.toVectorDrawable
import com.jzbrooks.vgo.util.xml.asSequence
import com.jzbrooks.vgo.vd.VectorDrawable
import com.jzbrooks.vgo.vd.VectorDrawableOptimizationRegistry
import com.jzbrooks.vgo.vd.VectorDrawableWriter
import com.jzbrooks.vgo.vd.toSvg
import org.w3c.dom.Document
import java.io.File
import java.util.jar.Manifest
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.roundToInt
import kotlin.system.exitProcess

class Application {
    private var printStats = false
    private var totalBytesBefore = 0.0
    private var totalBytesAfter = 0.0
    private var outputFormat: String? = null

    fun run(args: Array<String>): Int {
        val argReader = ArgReader(args.toMutableList())

        if (argReader.readFlag("help|h")) {
            println(HELP_MESSAGE)
            return 0
        }

        if (argReader.readFlag("version|v")) {
            val resources = this.javaClass.classLoader.getResources("META-INF/MANIFEST.MF")
            while (resources.hasMoreElements()) {
                val resource = resources.nextElement()
                val manifest = Manifest(resource?.openStream())
                manifest.mainAttributes.getValue("Bundle-Version")?.let(::println)
            }

            return 0
        }

        val writerOptions = mutableSetOf<Writer.Option>()
        argReader.readOption("indent")?.toIntOrNull()?.let { indentColumns ->
            writerOptions.add(Writer.Option.Indent(indentColumns))
        }

        printStats = argReader.readFlag("stats|s")

        val outputs = run {
            val outputPaths = mutableListOf<String>()
            var output = argReader.readOption("output|o")
            while (output != null) {
                outputPaths.add(output)
                output = argReader.readOption("output|o")
            }
            outputPaths.toList()
        }

        outputFormat = argReader.readOption("format")

        var inputs = argReader.readArguments()
        if (inputs.none()) {
            require(outputs.isEmpty())

            var path = readLine()
            val standardInPaths = mutableListOf<String>()
            while (path != null) {
                standardInPaths.add(path)
                path = readLine()
            }

            inputs = standardInPaths
        }

        val inputOutputPair = if (outputs.isNotEmpty()) {
            inputs.zip(outputs) { a, b ->
                Pair(File(a), File(b))
            }
        } else {
            inputs.zip(inputs) { a, b ->
                Pair(File(a), File(b))
            }
        }

        for ((input, output) in inputOutputPair) {
            if (input.isFile && (output.isFile || !output.exists())) {
                handleFile(input, output, writerOptions)
            } else if (input.isDirectory && (output.isDirectory || !output.exists())) {
                input.listFiles { file -> !file.isHidden }?.forEach { file ->
                    if (printStats) {
                        println("\n${file.path}")
                    }
                    handleFile(file, File(output, file.name), writerOptions)
                }
                if (printStats) {
                    val message = "| Total bytes saved: ${(totalBytesBefore - totalBytesAfter).roundToInt()} |"
                    val border = "-".repeat(message.length)
                    println("""
                        
                        $border
                        $message
                        $border
                    """.trimIndent())
                }
            } else {
                System.err.println("""
                    Input and output must be either files or directories.
                    Input is a ${if (input.isFile) "file" else "directory"}
                        path: ${input.absolutePath}
                        exists: ${input.exists()}
                        isWritable: ${input.canWrite()}
                    Output is a ${if (output.isFile) "file" else "directory"}
                        path: ${output.absolutePath}
                        exists: ${input.exists()}
                        isWritable: ${input.canWrite()}
                        
                    Storage: ${output.usableSpace} / ${output.totalSpace} is usable.
                """.trimIndent()
                )

                return 65
            }
        }

        return 0
    }

    private fun handleFile(input: File, output: File, options: Set<Writer.Option>) {
        input.inputStream().use { inputStream ->
            val sizeBefore = inputStream.channel.size()

            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input).apply {
                documentElement.normalize()
            }

            val rootNodes = document.childNodes.asSequence().filter { it.nodeType == Document.ELEMENT_NODE }.toList()
            var graphic = when {
                rootNodes.any { it.nodeName == "svg" || input.extension == "svg" } -> com.jzbrooks.vgo.svg.parse(rootNodes.first())
                rootNodes.any { it.nodeName == "vector" && input.extension == "xml"} -> com.jzbrooks.vgo.vd.parse(rootNodes.first())
                else -> null
            }

            if (graphic is VectorDrawable && outputFormat == "svg") {
                graphic = graphic.toSvg()
            }

            if (graphic is ScalableVectorGraphic && outputFormat == "vd") {
                graphic = graphic.toVectorDrawable()
            }

            val optimizationRegistry = when (graphic) {
                is VectorDrawable -> VectorDrawableOptimizationRegistry()
                is ScalableVectorGraphic -> SvgOptimizationRegistry()
                else -> null
            }

            if (graphic != null) {
                optimizationRegistry?.apply(graphic)
            }

            if (!output.parentFile.exists()) output.parentFile.mkdirs()
            if (!output.exists()) output.createNewFile()

            output.outputStream().use { outputStream ->
                if (graphic is VectorDrawable) {
                    val writer = VectorDrawableWriter(options)
                    writer.write(graphic, outputStream)
                }

                if (graphic is ScalableVectorGraphic){
                    val writer = ScalableVectorGraphicWriter(options)
                    writer.write(graphic, outputStream)
                }

                if (graphic == null && input != output) {
                    inputStream.copyTo(outputStream)
                }

                if (printStats) {
                    val sizeAfter = outputStream.channel.size()
                    val percentSaved = ((sizeBefore - sizeAfter) / sizeBefore.toDouble()) * 100
                    totalBytesBefore += sizeBefore
                    totalBytesAfter += sizeAfter
                    println("Size before: ${sizeBefore}B")
                    println("Size after: ${sizeAfter}B")
                    println("Percent saved: $percentSaved")
                }
            }
        }
    }

    companion object {
        private const val HELP_MESSAGE = """
> vgo [options] [file/directory]

Options:
  -h --help       print this message
  -o --output     file or directory, if not provided the input will be overwritten
  -s --stats      print statistics on processed files to standard out
  -v --version    print the version number
  --indent value  write files with value columns of indentation
  --format value  output format (svg, vd, etc)
        """

        @JvmStatic
        fun main(args: Array<String>): Unit = exitProcess(Application().run(args))
    }
}
