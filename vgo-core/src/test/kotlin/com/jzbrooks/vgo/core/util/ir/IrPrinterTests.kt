package com.jzbrooks.vgo.core.util.ir

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.util.element.createGraphic
import com.jzbrooks.vgo.core.util.element.createPath
import com.jzbrooks.vgo.core.util.math.Point
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

class IrPrinterTests {
    private val path =
        createPath(
            commands = listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 20f)))),
        )
    private val graphic = createGraphic(listOf(path))

    private fun captureIr(
        useColor: Boolean,
        block: (IrPrinter) -> Unit,
    ): String {
        val baos = ByteArrayOutputStream()
        IrPrinter(PrintStream(baos, true, StandardCharsets.UTF_8), useColor).also(block)
        return baos.toString(StandardCharsets.UTF_8)
    }

    @Test
    fun `asIr output contains root label`() {
        assertThat(graphic.asIr()).contains("Graphic")
    }

    @Test
    fun `asIr output contains path`() {
        assertThat(graphic.asIr()).contains("Path")
    }

    @Test
    fun `asIr output contains moveto command letter`() {
        assertThat(graphic.asIr()).contains("M")
    }

    @Test
    fun `plain mode produces no ansi escape codes`() {
        val esc = Char(0x1B).toString()
        assertThat(captureIr(false) { it.visit(graphic) }).doesNotContain(esc)
    }

    @Test
    fun `color mode produces ansi escape codes`() {
        val esc = Char(0x1B).toString()
        assertThat(captureIr(true) { it.visit(graphic) }).contains(esc)
    }

    @Test
    fun `graphic id is shown in brackets`() {
        val withId = createGraphic(id = "myicon")
        assertThat(captureIr(false) { it.visit(withId) }).contains("[myicon]")
    }

    @Test
    fun `path id is shown in brackets`() {
        val g = createGraphic(listOf(createPath(id = "mypath")))
        assertThat(captureIr(false) { it.visit(g) }).contains("[mypath]")
    }

    @Test
    fun `single child uses corner connector`() {
        assertThat(graphic.asIr()).contains("└──")
    }

    @Test
    fun `multiple children use branch and corner connectors`() {
        val g = createGraphic(listOf(path, path))
        val output = captureIr(false) { it.visit(g) }
        assertThat(output).contains("├──")
        assertThat(output).contains("└──")
    }

    @Test
    fun `group appears in output`() {
        val g = createGraphic(listOf(Group(listOf(path))))
        assertThat(captureIr(false) { it.visit(g) }).contains("Group")
    }
}
