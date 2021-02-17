import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("maven-publish")
    id("signing")
}

sourceSets {
    main {
        withConvention(KotlinSourceSet::class) {
            kotlin.srcDir("src/generated/kotlin")
        }
    }
}

dependencies {
    implementation(project(":vgo-core"))

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.23.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.6.0")
}

tasks {
    compileKotlin {
        dependsOn("generateConstants")
    }

    jar {
        dependsOn(configurations.runtimeClasspath)

        manifest {
            attributes["Main-Class"] = "com.jzbrooks.vgo.Application"
            attributes["Bundle-Version"] = project.version
        }

        val sourceClasses = sourceSets.main.get().output.classesDirs
        inputs.files(sourceClasses)
        destinationDirectory.set(file("$buildDir/libs/debug"))

        doFirst {
            from(files(sourceClasses))
            from(configurations.runtimeClasspath.get().asFileTree.files.map(::zipTree))

            exclude(
                    "**/*.kotlin_metadata",
                    "**/*.kotlin_module",
                    "**/*.kotlin_builtins",
                    "**/module-info.class",
                    "META-INF/maven/**",
                    "META-INF/*.version"
            )
        }
    }

    val generateConstants by registering {
        outputs.files("$projectDir/src/generated/kotlin/com/jzbrooks/BuildConstants.kt")

        doLast {
            val generatedDirectory = Paths.get("$projectDir/src/generated/kotlin/com/jzbrooks")
            Files.createDirectories(generatedDirectory)
            val generatedFile = generatedDirectory.resolve("BuildConstants.kt")

            PrintWriter(generatedFile.toFile()).use { output ->
                val buildConstantsClass = buildString {
                    appendln(
                        """
                               |package com.jzbrooks
                               |                           
                               |object BuildConstants {
                               """.trimMargin()
                    )


                    append("    const val ")
                    append("VERSION")
                    append(" = \"")
                    append(project.version)
                    appendln('"')

                    appendln("}")
                }
                output.write(buildConstantsClass)
            }
        }
    }

    val optimizedJar = file("$buildDir/libs/vgo.jar")
    val optimize by registering(JavaExec::class) {
        description = "Runs proguard on the jar application."
        group = "build"

        inputs.file("$buildDir/libs/debug/vgo-$version.jar")
        outputs.file(optimizedJar)

        val javaHome = System.getenv("JAVA_HOME") ?: javaInstalls
                .installationForCurrentVirtualMachine.get()
                .installationDirectory.toString()

        classpath = files("$rootDir/tools/r8.jar")
        args = listOf(
                "--release",
                "--classfile",
                "--lib", javaHome,
                "--output", "$buildDir/libs/vgo.jar",
                "--pg-conf", "$rootDir/optimize.pro",
                "$buildDir/libs/debug/vgo-$version.jar"
        )

        dependsOn(getByName("jar"))
    }

    val binaryFile = file("$buildDir/libs/vgo")
    val binary by registering {
        description = "Prepends shell script in the jar to improve CLI"
        group = "build"

        dependsOn(optimize)

        inputs.file(optimizedJar)
        outputs.file(binaryFile)

        doLast {
            binaryFile.parentFile.mkdirs()
            binaryFile.delete()
            binaryFile.appendText("#!/bin/sh\n\nexec java \$JAVA_OPTS -jar \$0 \"\$@\"\n\n")
            optimizedJar.inputStream().use { binaryFile.appendBytes(it.readBytes()) }
            binaryFile.setExecutable(true, false)
        }
    }

    val updateBaselineOptimizations by registering(Copy::class) {
        description = "Updates baseline assets with the latest integration test outputs."
        group = "Build Setup"

        from("$buildDir/test/") {
            include("*testOptimizationFinishes.xml")
            include("*testOptimizationFinishes.svg")
        }
        into("src/test/resources/baseline/")
        rename { original ->
            val originalAssetName = original.let {
                val i = it.lastIndexOf('_')
                it.substring(0 until i)
            }
            val fileExtension = original.split('.').last()
            "${originalAssetName}_optimized.$fileExtension"
        }
    }

    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    val javadocJar by creating(Jar::class) {
        archiveClassifier.set("javadoc")
        from(this@tasks["javadoc"])
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            artifactId = "vgo"

            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
            from(components["kotlin"])

            @Suppress("UnstableApiUsage")
            pom {
                name.set("vgo")
                description.set("vgo is a tool for optimizing vector artwork files that helps ensure your vector artwork is represented compactly without compromising quality.")
                url.set("https://github.com/jzbrooks/vgo/")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/jzbrooks/vgo/blob/master/LICENSE")
                    }
                }

                developers {
                    developer {
                        id.set("jzbrooks")
                        name.set("Justin Brooks")
                        email.set("justin@jzbrooks.com")
                    }
                }

                scm {
                    connection.set("scm:git:github.com/jzbrooks/vgo.git")
                    developerConnection.set("scm:git:ssh://github.com/jzbrooks/vgo.git")
                    url.set("https://github.com/jzbrooks/vgo/tree/master")
                }
            }
        }
    }

    repositories {
        maven {
            name = "sonatype"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("OSSRH_USERNAME")
                password = System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    sign(publishing.publications)
}
