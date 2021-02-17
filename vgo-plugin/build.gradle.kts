plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
    id("java-gradle-plugin")
    id("maven-publish")
    id("signing")
}

dependencies {
    implementation(project(":vgo"))

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.23.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
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
            artifactId = "vgo-plugin"

            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
            from(components["kotlin"])

            @Suppress("UnstableApiUsage")
            pom {
                name.set("vgo-plugin")
                description.set("vgo is a gradle plugin for optimizing vector artwork files that helps ensure a compact representation without compromising quality.")
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
