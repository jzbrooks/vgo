plugins {
    id("vgo.kotlin-conventions")
    id("org.jetbrains.intellij.platform")
}

version = rootProject.version

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
