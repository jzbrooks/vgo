import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    id("org.jetbrains.kotlin.jvm")
}

sourceSets {
    create("integrationTest") {
        withConvention(KotlinSourceSet::class) {
            kotlin.srcDir("src/integration-test/kotlin")
            resources.srcDir("src/integration-test/resources")
            compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
            runtimeClasspath += output + compileClasspath + sourceSets["test"].runtimeClasspath
        }
    }
}

dependencies {
    implementation(project(":vgo-core"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.21")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.6.0")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }

    withType<Jar> {
        destinationDirectory.set(file("$buildDir/libs/debug"))

        manifest {
            attributes["Main-Class"] = "com.jzbrooks.vgo.Application"
            attributes["Bundle-Version"] = "0.1.0"
        }

        dependsOn(configurations.runtimeClasspath)
        from(configurations.runtimeClasspath.get().filter { it.isFile }.map(::zipTree))
    }

    val optimize by registering {
        description = "Runs proguard on the jar application."
        group = "build"

        doLast {
            exec {
                workingDir(rootDir)
                commandLine("java", "-jar", "$rootDir/tools/proguard.jar", "@$rootDir/optimize.pro")
            }
        }
        dependsOn(getByName("jar"))
    }

    val integrationTest by registering(Test::class) {
        description = "Runs the integration tests."
        group = "verification"
        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath
        mustRunAfter(getByName("test"))
        dependsOn(getByName("jar"))
    }

    getByName("check").dependsOn(integrationTest)
}
