import org.jetbrains.changelog.Changelog

plugins {
    id("vgo.kotlin-conventions")
    id("org.jetbrains.intellij.platform")
    alias(libs.plugins.changelog)
}

version = providers.gradleProperty("VERSION_NAME").get()

// The marketplace listing takes its release notes from the repository changelog.
// The root project owns that file, so the tasks that read or rewrite it are disabled
// here to keep `gradlew patchChangelog` and `gradlew getChangelog` single-writer.
val changelogFile = layout.projectDirectory.file("../changelog.md")

changelog {
    path = changelogFile.asFile.path
}

listOf("patchChangelog", "getChangelog", "initializeChangelog").forEach { changelogTask ->
    tasks.named(changelogTask) { enabled = false }
}

// Rendering eagerly keeps script references out of the configuration cache entry. Reading the
// file through a provider registers it as a configuration cache input, so editing the changelog
// refreshes the notes.
val changeNotesHtml =
    providers
        .fileContents(changelogFile)
        .asText
        .map {
            val item = changelog.getOrNull(version.toString()) ?: changelog.getUnreleased()
            changelog.renderItem(
                item.withHeader(false).withEmptySections(false),
                Changelog.OutputType.HTML,
            )
        }.get()

repositories {
    mavenCentral()
    google()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation(project(":vgo"))

    intellijPlatform {
        intellijIdeaCommunity("2024.3")
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "com.jzbrooks.vgo"
        name = "vgo"
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "243"
            untilBuild = provider { null }
        }
        vendor {
            name = "jzbrooks"
        }
        description = "Optimize SVG and Android VectorDrawable files with vgo from the Project view context menu."
        changeNotes = changeNotesHtml
    }

    signing {
        certificateChain.set(providers.gradleProperty("intellijCertificateChain"))
        privateKey.set(providers.gradleProperty("intellijPrivateKey"))
        password.set(providers.gradleProperty("intellijPrivateKeyPassword"))
    }

    publishing {
        token.set(providers.gradleProperty("intellijPublishToken"))
    }
}
