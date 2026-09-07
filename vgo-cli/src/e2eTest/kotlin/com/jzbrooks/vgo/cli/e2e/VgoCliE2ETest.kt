package com.jzbrooks.vgo.cli.e2e

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.exists
import assertk.assertions.hasText
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.startsWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.createDirectories
import kotlin.io.path.fileSize
import kotlin.io.path.readBytes
import kotlin.io.path.readText

/**
 * Black box coverage for the shipped CLI.
 *
 * Every assertion here is reachable only through a real process: the artifact
 * under test is the R8 optimized binary, so these are the only tests that can
 * catch a missing keep rule, a broken manifest, or reflection that survived
 * compilation but not shrinking. Behavior that doesn't depend on packaging
 * belongs in CommandLineInterfaceTests instead, which is far cheaper to run.
 */
class VgoCliE2ETest {
    @TempDir
    lateinit var workingDirectory: Path

    private val escape = Char(0x1B).toString()

    // The distribution itself

    @Test
    fun `version flag reports the version the build stamped in`() {
        val invocation = VgoBinary.run("--version", workingDirectory = workingDirectory)

        assertThat(invocation.exitCode, "$invocation").isEqualTo(0)
        assertThat(invocation.stdout.trim()).isEqualTo(VgoBinary.version)
    }

    @Test
    fun `help flag documents every option`() {
        val invocation = VgoBinary.run("--help", workingDirectory = workingDirectory)

        assertThat(invocation.exitCode, "$invocation").isEqualTo(0)
        for (option in DOCUMENTED_OPTIONS) {
            assertThat(invocation.stdout, "help text").contains(option)
        }
    }

    @Test
    fun `missing input reports a usage error`() {
        val invocation = VgoBinary.run(workingDirectory = workingDirectory)

        assertThat(invocation.exitCode, "$invocation").isEqualTo(64)
        assertThat(invocation.stderr).contains("No input files or directories were provided")
    }

    // Optimization output is unchanged by shrinking.
    //
    // The baselines are the vgo module's goldens, written by the library at
    // indent 2 (see BaselineTests). Comparing the binary against them proves R8
    // rewrote the code without changing what it computes.

    @Test
    fun `vector drawable optimization matches the library baseline`() {
        assertMatchesBaseline("avocado_example.xml", "baseline/avocado_example_optimized.xml")
    }

    @Test
    fun `svg optimization matches the library baseline`() {
        assertMatchesBaseline("tiger.svg", "baseline/tiger_optimized.svg")
    }

    // Conversions. No goldens exist for these, so assert the output is the
    // right format and well formed.

    @Test
    fun `vector drawable converts to svg`() {
        val input = VgoBinary.stage("avocado_example.xml", workingDirectory)
        val output = workingDirectory.resolve("converted.svg")

        val invocation =
            VgoBinary.run(
                input.toString(),
                "-o",
                output.toString(),
                "--format",
                "svg",
                workingDirectory = workingDirectory,
            )

        assertThat(invocation.exitCode, "$invocation").isEqualTo(0)
        assertThat(output.readText()).startsWith("<svg")
        assertParses(output)
    }

    @Test
    fun `svg with clip paths converts to a vector drawable`() {
        // guacamole.svg exercises clipPath and clip-rule, so the conversion
        // routes through com.android.ide.common.vectordrawable — the dependency
        // optimize.pro has had to hand-keep parts of.
        val input = VgoBinary.stage("guacamole.svg", workingDirectory)
        val output = workingDirectory.resolve("converted.xml")

        val invocation =
            VgoBinary.run(
                input.toString(),
                "-o",
                output.toString(),
                "--format",
                "vd",
                workingDirectory = workingDirectory,
            )

        assertThat(invocation.exitCode, "$invocation").isEqualTo(0)
        assertThat(output.readText()).startsWith("<vector")
        assertParses(output)
    }

    // The CLI surface, across a process boundary

    @Test
    fun `stats flag reports savings`() {
        val input = VgoBinary.stage("avocado_example.xml", workingDirectory)
        val output = workingDirectory.resolve("optimized.xml")

        val invocation =
            VgoBinary.run(
                input.toString(),
                "-o",
                output.toString(),
                "--stats",
                workingDirectory = workingDirectory,
            )

        assertThat(invocation.exitCode, "$invocation").isEqualTo(0)
        assertThat(invocation.stdout).contains("Percent saved:")
    }

    @Test
    fun `input is optimized in place when no output is given`() {
        val input = VgoBinary.stage("avocado_example.xml", workingDirectory)
        val sizeBefore = input.fileSize()

        val invocation = VgoBinary.run(input.toString(), workingDirectory = workingDirectory)

        assertThat(invocation.exitCode, "$invocation").isEqualTo(0)
        assertThat(input.fileSize(), "optimized size").isLessThan(sizeBefore)
    }

    @Test
    fun `directory input writes every graphic to the output directory`() {
        val inputDirectory = workingDirectory.resolve("in").createDirectories()
        VgoBinary.stage("avocado_example.xml", inputDirectory)
        VgoBinary.stage("simple_heart.xml", inputDirectory)
        val outputDirectory = workingDirectory.resolve("out")

        val invocation =
            VgoBinary.run(
                inputDirectory.toString(),
                "-o",
                outputDirectory.toString(),
                workingDirectory = workingDirectory,
            )

        assertThat(invocation.exitCode, "$invocation").isEqualTo(0)
        assertThat(outputDirectory.resolve("avocado_example.xml")).exists()
        assertThat(outputDirectory.resolve("simple_heart.xml")).exists()
    }

    @Test
    fun `stdin flag reads newline-delimited paths`() {
        val first = VgoBinary.stage("avocado_example.xml", workingDirectory)
        val second = VgoBinary.stage("simple_heart.xml", workingDirectory)
        val sizeBefore = first.fileSize()

        val invocation =
            VgoBinary.run(
                "--stdin",
                workingDirectory = workingDirectory,
                stdin = "$first\n$second\n",
            )

        assertThat(invocation.exitCode, "$invocation").isEqualTo(0)
        assertThat(first.fileSize(), "optimized size").isLessThan(sizeBefore)
    }

    @Test
    fun `check flag reports an unshrunk file without modifying it`() {
        val input = VgoBinary.stage("avocado_example.xml", workingDirectory)
        val bytesBefore = input.readBytes()

        val invocation = VgoBinary.run(input.toString(), "--check", workingDirectory = workingDirectory)

        assertThat(invocation.exitCode, "$invocation").isEqualTo(1)
        assertThat(invocation.stdout).contains(input.toString())
        assertThat(input.readBytes().contentEquals(bytesBefore), "file unchanged").isEqualTo(true)
    }

    @Test
    fun `check flag accepts a fully shrunk file`() {
        // simple_heart reaches a fixed point in a single pass. Most assets,
        // avocado included, shrink again on a second run, so they can't be used
        // here. See the note about idempotence in the readme.
        val input = VgoBinary.stage("simple_heart.xml", workingDirectory)
        val shrunk = workingDirectory.resolve("shrunk.xml")

        val optimize =
            VgoBinary.run(input.toString(), "-o", shrunk.toString(), workingDirectory = workingDirectory)
        assertThat(optimize.exitCode, "$optimize").isEqualTo(0)

        val invocation = VgoBinary.run(shrunk.toString(), "--check", workingDirectory = workingDirectory)

        assertThat(invocation.exitCode, "$invocation").isEqualTo(0)
    }

    @Test
    fun `indent option is honored`() {
        val input = VgoBinary.stage("avocado_example.xml", workingDirectory)
        val output = workingDirectory.resolve("indented.xml")

        val invocation =
            VgoBinary.run(
                input.toString(),
                "-o",
                output.toString(),
                "--indent",
                "4",
                workingDirectory = workingDirectory,
            )

        assertThat(invocation.exitCode, "$invocation").isEqualTo(0)
        assertThat(output.readText()).contains("    <path")
    }

    // Terminal detection. ColorSupport reflectively looks up
    // java.io.Console.isTerminal, which R8 can't see and the in-process tests
    // can't reach, since they never have a real process to redirect.

    @Test
    fun `ir dump is plain when stdout is redirected`() {
        val input = VgoBinary.stage("simple_heart.xml", workingDirectory)

        val invocation = VgoBinary.run(input.toString(), "--print-ir", workingDirectory = workingDirectory)

        assertThat(invocation.exitCode, "$invocation").isEqualTo(0)
        assertThat(invocation.stdout).contains("Path")
        assertThat(invocation.stdout).doesNotContain(escape)
    }

    @Test
    fun `ir dump is colored when color is forced`() {
        val input = VgoBinary.stage("simple_heart.xml", workingDirectory)

        val invocation =
            VgoBinary.run(
                input.toString(),
                "--print-ir",
                workingDirectory = workingDirectory,
                env = mapOf("CLICOLOR_FORCE" to "1"),
            )

        assertThat(invocation.exitCode, "$invocation").isEqualTo(0)
        assertThat(invocation.stdout).contains(escape)
    }

    @Test
    fun `no color wins over forced color`() {
        val input = VgoBinary.stage("simple_heart.xml", workingDirectory)

        val invocation =
            VgoBinary.run(
                input.toString(),
                "--print-ir",
                workingDirectory = workingDirectory,
                env = mapOf("NO_COLOR" to "1", "CLICOLOR_FORCE" to "1"),
            )

        assertThat(invocation.exitCode, "$invocation").isEqualTo(0)
        assertThat(invocation.stdout).contains("Path")
        assertThat(invocation.stdout).doesNotContain(escape)
    }

    private fun assertMatchesBaseline(
        asset: String,
        baseline: String,
    ) {
        val input = VgoBinary.stage(asset, workingDirectory)
        val output = workingDirectory.resolve("optimized-$asset")

        val invocation =
            VgoBinary.run(
                input.toString(),
                "-o",
                output.toString(),
                "--indent",
                "2",
                workingDirectory = workingDirectory,
            )

        assertThat(invocation.exitCode, "$invocation").isEqualTo(0)
        assertThat(output.toFile(), "optimized $asset")
            .hasText(VgoBinary.corpus.resolve(baseline).readText())
    }

    private fun assertParses(document: Path) {
        DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(document.toFile())
    }

    private companion object {
        val DOCUMENTED_OPTIONS =
            listOf(
                "vgo",
                "--help",
                "--output",
                "--stats",
                "--version",
                "--indent",
                "--format",
                "--no-optimization",
                "--stdin",
                "--check",
                "--print-ir",
            )
    }
}
