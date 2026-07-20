package com.jzbrooks.vgo.plugin

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.prop
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

class VgoPluginTest {
    @Test
    fun pluginAddsTaskToProject() {
        val project: Project = ProjectBuilder.builder().build()
        project.tasks.register("check")
        project.pluginManager.apply("com.jzbrooks.vgo")
        val task = project.tasks.getByName("shrinkVectorGraphic")
        assertThat(task).isInstanceOf(ShrinkVectorGraphic::class)
    }

    @Test
    fun pluginAddsCheckTaskToProject() {
        val project: Project = ProjectBuilder.builder().build()
        project.tasks.register("check")
        project.pluginManager.apply("com.jzbrooks.vgo")
        val task = project.tasks.getByName("checkVectorGraphic")
        assertThat(task).isInstanceOf(CheckVectorGraphic::class)
    }

    @Test
    fun testConfiguration() {
        val project: Project =
            ProjectBuilder
                .builder()
                .withProjectDir(File("src/test"))
                .build()

        project.tasks.register("check")

        val input = File(project.projectDir, "kotlin/com/jzbrooks/vgo/plugin/VgoPluginTest.kt")

        project.pluginManager.apply("com.jzbrooks.vgo")
        val extension = project.extensions.getByType(VgoPluginExtension::class.java)
        extension.inputs.setFrom(project.fileTree(input))

        val task = project.tasks.getByName("shrinkVectorGraphic") as ShrinkVectorGraphic

        assertThat(task.inputFiles.files).containsOnly(input)
        assertThat(task.showStatistics.get()).isTrue()
    }

    @Test
    fun outputsDefaultToInputsForInPlaceOptimization() {
        val project: Project =
            ProjectBuilder
                .builder()
                .withProjectDir(File("src/test"))
                .build()

        project.tasks.register("check")

        val input = File(project.projectDir, "kotlin/com/jzbrooks/vgo/plugin/VgoPluginTest.kt")

        project.pluginManager.apply("com.jzbrooks.vgo")
        val extension = project.extensions.getByType(VgoPluginExtension::class.java)
        extension.inputs.setFrom(project.fileTree(input))

        val task = project.tasks.getByName("shrinkVectorGraphic") as ShrinkVectorGraphic

        assertThat(task.outputFiles.files).containsOnly(input)
    }

    @Test
    fun explicitOutputsAreUsedWhenConfigured() {
        val project: Project =
            ProjectBuilder
                .builder()
                .withProjectDir(File("src/test"))
                .build()

        project.tasks.register("check")

        val input = File(project.projectDir, "kotlin/com/jzbrooks/vgo/plugin/VgoPluginTest.kt")
        val output = File(project.projectDir, "converted/VgoPluginTest.xml")

        project.pluginManager.apply("com.jzbrooks.vgo")
        val extension = project.extensions.getByType(VgoPluginExtension::class.java)
        extension.inputs.setFrom(project.fileTree(input))
        extension.outputs.setFrom(output)

        val task = project.tasks.getByName("shrinkVectorGraphic") as ShrinkVectorGraphic

        assertThat(task.outputFiles.files).containsOnly(output)
    }

    @Test
    fun checkTaskConfiguration() {
        val project: Project =
            ProjectBuilder
                .builder()
                .withProjectDir(File("src/test"))
                .build()

        project.tasks.register("check")

        val input = File(project.projectDir, "kotlin/com/jzbrooks/vgo/plugin/VgoPluginTest.kt")

        project.pluginManager.apply("com.jzbrooks.vgo")
        val extension = project.extensions.getByType(VgoPluginExtension::class.java)
        extension.inputs.setFrom(project.fileTree(input))

        val task = project.tasks.getByName("checkVectorGraphic")

        assertThat(task)
            .isInstanceOf<CheckVectorGraphic>()
            .prop(CheckVectorGraphic::files)
            .containsExactly(input.absolutePath)
    }
}
