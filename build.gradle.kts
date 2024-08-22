import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.KtlintPlugin

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.20"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("com.vanniktech.maven.publish") version "0.29.0"
    id("org.jetbrains.changelog") version "2.2.1"
}

version = property("VERSION_NAME").toString()

changelog.path.set("changelog.md")

subprojects {
    apply<KtlintPlugin>()
    configure<KtlintExtension> {
        version.set("1.2.1")
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        configure<KotlinJvmProjectExtension> {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
        }

        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()

        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
        }

        addTestListener(
            object : TestListener {
                override fun afterSuite(
                    suite: TestDescriptor,
                    result: TestResult,
                ) {
                    if (suite.parent == null) {
                        val output =
                            buildString {
                                append("|  Results: ")
                                append(result.resultType)
                                append(" (")
                                append(result.testCount)
                                append(" tests, ")
                                append(result.successfulTestCount)
                                append(" passed, ")
                                append(result.failedTestCount)
                                append(" failed, ")
                                append(result.skippedTestCount)
                                append(" skipped)  |")
                            }
                        val border = "-".repeat(output.length)
                        logger.lifecycle(
                            """
                            $border
                            $output
                            $border
                            """.trimIndent(),
                        )
                    }
                }

                override fun afterTest(
                    testDescriptor: TestDescriptor?,
                    result: TestResult?,
                ) {}

                override fun beforeTest(testDescriptor: TestDescriptor?) {}

                override fun beforeSuite(suite: TestDescriptor?) {}
            },
        )
    }
}
