plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.17.0"
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.1")
}

tasks {
    val sourcesJar by registering(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    val javadocJar by registering(Jar::class) {
        archiveClassifier.set("javadoc")
        from(this@tasks["javadoc"])
    }
}
