package com.jzbrooks.guacamole.vd

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jzbrooks.guacamole.App
import org.junit.Test
import java.io.File

class AvocadoExampleTests {
    private val avocadoExampleRelativePath = "src/integration-test/resources/avocado_example.xml"

    @Test
    fun testOptimizationFinishes() {
        val arguments = arrayOf(avocadoExampleRelativePath, "-o", "build/integrationTest/test.xml")

        val exitCode = App().run(arguments)

        assertThat(exitCode).isEqualTo(0)
    }

    @Test
    fun testOptimizationIsCompact() {
        val arguments = arrayOf(avocadoExampleRelativePath, "-o", "build/integrationTest/test.xml")

        App().run(arguments)

        val content = File("build/integrationTest/test.xml").readText()
        assertThat(content).isEqualTo(expected)
    }

    companion object {
        const val expected = """<vector xmlns:android="http://schemas.android.com/apk/res/android" android:height="108dp" android:viewportHeight="108" android:viewportWidth="108" android:width="108dp"><path android:fillColor="#26A69A" android:pathData="M0,0h108v108H0Z"/><path android:fillColor="#00000000" android:pathData="M-13.5,-27l0,162M1.5,-27l0,162m15,-162l0,162m15,-162l0,162m15,-162l0,162m15,-162l0,162M69,0l0,108M79,0l0,108M89,0l0,108M99,0l0,108m63,45L54,152.99998M162,143L54,142.99998M162,133L54,132.99998M162,123L54,122.99999M162,113L54.000008,112.99999M162,103L54.000008,102.99999M54,123l108,0M54,133l108,0M54,143l108,0M54,153l108,0M73,83l70,0M73,93l70,0M73,103l70,0M73,113l70,0M73,123l70,0M73,133l70,0M83,73l0,70M93,73l0,70M103,73l0,70M113,73l0,70M123,73l0,70M133,73l0,70" android:strokeColor="#33FFFFFF" android:strokeWidth="0.8"/></vector>"""
    }
}