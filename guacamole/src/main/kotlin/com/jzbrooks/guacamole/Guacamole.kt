package com.jzbrooks.guacamole

import com.jzbrooks.guacamole.core.Writer
import com.jzbrooks.guacamole.svg.ScalableVectorGraphic
import com.jzbrooks.guacamole.svg.ScalableVectorGraphicWriter
import com.jzbrooks.guacamole.vd.VectorDrawable
import com.jzbrooks.guacamole.vd.VectorDrawableWriter
import java.io.File
import java.util.jar.Manifest
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.system.exitProcess

class Guacamole {
    private var printStats = false

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
                input.listFiles { file -> !file.isHidden }?.forEach { handleFile(it, File(output, it.name), writerOptions) }
            } else {
                System.err.println("Input and output must be either files or directories.")
                System.err.println("Input is a " + if (input.isFile) "file" else "directory")
                System.err.println("Output is a " + if (output.isFile) "file" else "directory")
                return 1
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

            val graphic = if (document.firstChild.nodeName == "svg") {
                com.jzbrooks.guacamole.svg.parse(document)
            } else {
                com.jzbrooks.guacamole.vd.parse(document)
            }

            val optimizationRegistry = when (graphic) {
                is VectorDrawable -> com.jzbrooks.guacamole.vd.OptimizationRegistry()
                is ScalableVectorGraphic -> com.jzbrooks.guacamole.svg.OptimizationRegistry()
                else -> null
            }

            optimizationRegistry?.apply(graphic)

            if (!output.parentFile.exists()) output.parentFile.mkdirs()
            if (!output.exists()) output.createNewFile()

            output.outputStream().use { outputStream ->
                if (graphic is VectorDrawable) {
                    val writer = VectorDrawableWriter(options)
                    writer.write(graphic, outputStream)
                } else {
                    val writer = ScalableVectorGraphicWriter(options)
                    writer.write(graphic, outputStream)
                }

                if (printStats) {
                    val sizeAfter = outputStream.channel.size()
                    val percentSaved = ((sizeBefore - sizeAfter) / sizeBefore.toDouble()) * 100
                    println("Size before: $sizeBefore")
                    println("Size after: $sizeAfter")
                    println("Percent saved: $percentSaved")
                }
            }
        }
    }

    companion object {
        private const val HELP_MESSAGE = """
> guacamole [options] [file/directory]

Options:
  -h --help       print this message
  -o --output     file or directory, if not provided the input will be overwritten
  -s --stats      print statistics on processed files to standard out
  -v --version    print the version number
  --indent=value  write files with value columns of indentation 
        """

        @JvmStatic
        fun main(args: Array<String>): Unit = exitProcess(Guacamole().run(args))
    }
}
