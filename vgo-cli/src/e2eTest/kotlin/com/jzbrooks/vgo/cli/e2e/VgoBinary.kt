package com.jzbrooks.vgo.cli.e2e

import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Drives the packaged CLI — the R8 optimized artifact produced by the `binary`
 * task — as a subprocess.
 *
 * These tests are deliberately black box. Nothing here links against vgo-cli
 * classes, because the point is to exercise the shrunk, shipped bytes rather
 * than the ones the compiler emitted.
 */
object VgoBinary {
    /** The corpus shared with the vgo module's baseline tests. */
    val corpus: Path = Path.of(requiredProperty("vgo.corpus"))

    /** Pre-optimized golden files, written by the library at `--indent 2`. */
    val baseline: Path = corpus.resolve("baseline")

    /** The version the build stamped into the artifact. */
    val version: String = requiredProperty("vgo.version")

    private val command: List<String> =
        if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
            // The binary is a jar behind a `#!/bin/sh` header, which Windows
            // can't execute. The readme documents `java -jar` there.
            listOf(javaExecutable(), "-jar", requiredProperty("vgo.jar"))
        } else {
            listOf(requiredProperty("vgo.binary"))
        }

    fun run(
        vararg arguments: String,
        workingDirectory: Path,
        env: Map<String, String> = emptyMap(),
        stdin: String? = null,
    ): Invocation {
        val stdoutFile = workingDirectory.resolve("stdout.txt")
        val stderrFile = workingDirectory.resolve("stderr.txt")

        val builder =
            ProcessBuilder(command + arguments)
                .directory(workingDirectory.toFile())
                .redirectOutput(stdoutFile.toFile())
                .redirectError(stderrFile.toFile())

        // Inherited color variables would make --print-ir assertions depend on
        // whoever launched the build.
        val processEnvironment = builder.environment()
        processEnvironment.keys.removeAll(COLOR_VARIABLES)
        processEnvironment.putAll(env)

        val process = builder.start()

        // Closing stdin matters even when nothing is written: --stdin reads
        // until end of stream.
        process.outputStream.use { stream ->
            if (stdin != null) stream.write(stdin.toByteArray())
        }

        if (!process.waitFor(2, TimeUnit.MINUTES)) {
            process.destroyForcibly()
            throw AssertionError(
                "vgo ${arguments.joinToString(" ")} did not exit within two minutes.",
            )
        }

        return Invocation(
            exitCode = process.exitValue(),
            stdout = stdoutFile.readText(),
            stderr = stderrFile.readText(),
        )
    }

    /** Copies a corpus asset into [workingDirectory] so runs never mutate fixtures. */
    fun stage(
        asset: String,
        workingDirectory: Path,
        name: String = Path.of(asset).fileName.toString(),
    ): Path {
        val staged = workingDirectory.resolve(name)
        staged.writeText(corpus.resolve(asset).readText())
        return staged
    }

    private val COLOR_VARIABLES = setOf("NO_COLOR", "CLICOLOR_FORCE", "TERM")

    private fun javaExecutable(): String =
        Path
            .of(System.getProperty("java.home"), "bin", "java")
            .absolutePathString()

    private fun requiredProperty(name: String): String =
        checkNotNull(System.getProperty(name)) {
            "The $name system property is set by the e2eTest task. Run `./gradlew :vgo-cli:e2eTest`."
        }
}

data class Invocation(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
) {
    /** Included in assertion failures, where the process output is the only clue. */
    override fun toString(): String =
        buildString {
            appendLine("exit code: $exitCode")
            appendLine("stdout:")
            appendLine(stdout)
            appendLine("stderr:")
            append(stderr)
        }
}
