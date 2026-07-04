plugins {
    alias(libs.plugins.changelog)
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.intellij.platform) apply false
}

version = property("VERSION_NAME").toString()

changelog.path.set("changelog.md")
