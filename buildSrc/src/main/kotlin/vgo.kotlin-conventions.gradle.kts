import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

val libs = the<VersionCatalogsExtension>().named("libs")

ktlint {
    version.set(libs.findVersion("ktlint-cli").get().requiredVersion)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        val runningFromIdea =
            System.getProperty("idea.active") == "true" ||
                System.getProperty("idea.sync.active") == "true"
        allWarningsAsErrors.set(!runningFromIdea)
        extraWarnings.set(!runningFromIdea)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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
