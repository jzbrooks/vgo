package com.jzbrooks.vgo.plugin

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTreeElement
import org.gradle.api.specs.Spec

/**
 * Excludes everything beneath a project's build directory, which is only known
 * once the tree is resolved because the build directory can be relocated after
 * the plugin is applied.
 */
internal class BuildDirectoryExclusion(
    private val buildDirectory: DirectoryProperty,
) : Spec<FileTreeElement> {
    override fun isSatisfiedBy(element: FileTreeElement): Boolean = element.file.toPath().startsWith(buildDirectory.get().asFile.toPath())
}
