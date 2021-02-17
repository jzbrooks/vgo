package com.jzbrooks.vgo.plugin

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByName
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

class VgoPluginTest {
    @Test
    fun pluginAddsTaskToProject() {
        val project: Project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.jzbrooks.vgo")
        val task = project.tasks.getByName("shrinkVectorArtwork")
        assertThat(task).isInstanceOf(ShrinkVectorArtwork::class)
    }

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

        val task = project.tasks.getByName<ShrinkVectorArtwork>("shrinkVectorArtwork")

        assertThat(task.files).containsExactly(input.absolutePath)
        assertThat(task.showStatistics).isTrue()
    }
}