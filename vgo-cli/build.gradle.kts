plugins {
    id("org.jetbrains.kotlin.jvm")
}

tasks {
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        dependsOn(configurations.runtimeClasspath)
        manifest {
            attributes["Main-Class"] = "com.jzbrooks.vgo.cli.CommandLineInterface"
            attributes["Bundle-Version"] = project.properties["VERSION_NAME"]
        }

        val sourceClasses =
            sourceSets.main
                .get()
                .output.classesDirs
        inputs.files(sourceClasses)
        destinationDirectory.set(layout.buildDirectory.dir("libs/debug"))

        doFirst {
            from(files(sourceClasses))
            from(
                configurations.runtimeClasspath
                    .get()
                    .asFileTree.files
                    .map(::zipTree),
            )
        }
    }

    val optimize by registering(JavaExec::class) {
        description = "Runs r8 on the jar application."
        group = "build"

        inputs.file("build/libs/debug/vgo-cli.jar")
        outputs.file("build/libs/vgo.jar")

        val javaHome = System.getProperty("java.home")

        classpath(r8)
        mainClass = "com.android.tools.r8.R8"

        args(
            "--release",
            "--classfile",
            "--lib",
            javaHome,
            "--output",
            "build/libs/vgo.jar",
            "--pg-conf",
            "optimize.pro",
            "build/libs/debug/vgo-cli.jar",
        )

        dependsOn(getByName("jar"))
    }

    val binaryFileProp = layout.buildDirectory.file("libs/vgo")
    val binary by registering {
        description = "Prepends shell script in the jar to improve CLI"
        group = "build"

        dependsOn(optimize)

        inputs.file("build/libs/vgo.jar")
        outputs.file(binaryFileProp)

        doLast {
            val binaryFile = binaryFileProp.get().asFile
            binaryFile.parentFile.mkdirs()
            binaryFile.delete()
            binaryFile.appendText("#!/bin/sh\n\nexec java \$JAVA_OPTS -jar \$0 \"\$@\"\n\n")
            file("build/libs/vgo.jar").inputStream().use { binaryFile.appendBytes(it.readBytes()) }
            binaryFile.setExecutable(true, false)
        }
    }
}

val r8: Configuration by configurations.creating

dependencies {
    implementation(project(":vgo"))

    implementation("com.android.tools:sdk-common:31.9.0")

    r8("com.android.tools:r8:8.7.18")

    testImplementation(platform("org.junit:junit-bom:5.12.1"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.1")
}
