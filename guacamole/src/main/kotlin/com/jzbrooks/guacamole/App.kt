package com.jzbrooks.guacamole

import com.jzbrooks.guacamole.core.Writer
import com.jzbrooks.guacamole.core.optimization.*
import com.jzbrooks.guacamole.vd.VectorDrawableWriter
import com.jzbrooks.guacamole.vd.parse
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.util.jar.Manifest

class App {
    private var printStats = false

    fun run(args: Array<String>) {
        val argReader = ArgReader(args.toMutableList())

        if (argReader.readFlag("help|h")) {
            println(HELP_MESSAGE)
            return
        }

        if (argReader.readFlag("version|v")) {
            val resources = this.javaClass.classLoader.getResources("META-INF/MANIFEST.MF")
            while (resources.hasMoreElements()) {
                val resource = resources.nextElement()
                val manifest = Manifest(resource?.openStream())
                manifest.mainAttributes.getValue("Bundle-Version")?.let(::println)
            }

            return
        }

        val writerOptions = mutableSetOf<Writer.Option>()
        argReader.readOption("indent")?.toIntOrNull()?.let { indentColumns ->
            writerOptions.add(Writer.Option.Indent(indentColumns))
        }

        printStats = argReader.readFlag("stats|s")

        val writer = VectorDrawableWriter(writerOptions)

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
                handleFile(input, output, writer)
            } else if (input.isDirectory && (output.isDirectory || !output.exists())) {
                input.listFiles { file -> !file.isHidden }?.forEach { handleFile(it, File(output, it.name), writer) }
            } else {
                System.err.println("Input and output must be either files or directories.")
                System.err.println("Input is a " + if (input.isFile) "file" else "directory")
                System.err.println("Output is a " + if (output.isFile) "file" else "directory")
            }
        }
    }

    private fun handleFile(input: File, output: File, writer: Writer) {
        FileInputStream(input).use { inputStream ->
            val vectorDrawable = ByteArrayInputStream(inputStream.readBytes()).use(::parse)

            vectorDrawable.optimizationRegistry.optimizations.forEach { it.optimize(vectorDrawable) }

            if (!output.parentFile.exists()) output.parentFile.mkdirs()
            if (!output.exists()) output.createNewFile()

            output.outputStream().use { outputStream ->
                writer.write(vectorDrawable, outputStream)

                if (printStats) {
                    val sizeBefore = inputStream.channel.size()
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
        fun main(args: Array<String>) = App().run(args)
    }
}
