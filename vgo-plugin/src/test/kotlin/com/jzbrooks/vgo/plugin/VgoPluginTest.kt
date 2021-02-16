package com.jzbrooks.vgo.plugin

import assertk.assertThat
import assertk.assertions.containsExactly
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

class VgoPluginTest {
    @Test
    fun pluginAddsTaskToProject() {
        val project: Project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.jzbrooks.vgo")
        project.tasks.getByName("shrinkVectorArtwork")
    }

    @Disabled("Needs a custom task first (to get the output out of the task)")
    @Test
    fun testConfiguration() {
        val project: Project = ProjectBuilder.builder()
            .withProjectDir(File("src/test"))
            .build()

        val input = File(project.projectDir, "kotlin/com/jzbrooks/vgo/plugin/VgoPluginTest.kt")

        project.pluginManager.apply("com.jzbrooks.vgo")
        project.configure<VgoPluginExtension> {
            inputs = project.fileTree(input)
        }
        val task = project.tasks.getByName("shrinkVectorArtwork")

//        assertThat(task.args).containsExactly(input.absolutePath, "--stats")
    }
}