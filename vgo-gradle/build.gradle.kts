plugins {
    id("org.jetbrains.kotlin.jvm")
    id("java-gradle-plugin")
    id("com.vanniktech.maven.publish")
}

dependencies {
    implementation(gradleKotlinDsl())
    implementation(project(":vgo"))

    testImplementation(platform("org.junit:junit-bom:6.1.1"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.1")
}

gradlePlugin {
    plugins {
        register("VgoPlugin") {
            id = "com.jzbrooks.vgo"
            implementationClass = "com.jzbrooks.vgo.plugin.VgoPlugin"
        }
    }
}

tasks {
    register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    register<Jar>("javadocJar") {
        archiveClassifier.set("javadoc")
        from(this@tasks["javadoc"])
    }
}
