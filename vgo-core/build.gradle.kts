plugins {
    id("org.jetbrains.kotlin.jvm")
    id("maven-publish")
    id("signing")
}

dependencies {
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.22")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.6.0")
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
            artifactId = "vgo-core"

            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
            from(components["kotlin"])

            @Suppress("UnstableApiUsage")
            pom {
                name.set("vgo-core")
                description.set("vgo-core is a library for optimizing vector artwork files.")
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
