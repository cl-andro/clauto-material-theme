subprojects {
    group = "com.clauto.tools.materialthemebuilder"
    val rawSuiteVersion = if (project.hasProperty("suiteVersion")) project.property("suiteVersion").toString() else ""
    val cleanSuiteVersion = if (rawSuiteVersion.startsWith("v")) rawSuiteVersion.substring(1) else rawSuiteVersion
    version = if (cleanSuiteVersion.isNotEmpty()) cleanSuiteVersion else "1.5.1"

    plugins.withId("java") {
        println("- Configuring `java`")

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        tasks.register("sourcesJar", type = Jar::class) {
            archiveClassifier.set("sources")
            from(project.extensions.getByType<SourceSetContainer>().getByName("main").allSource)
        }
        tasks.register("javadocJar", type = Jar::class) {
            archiveClassifier.set("javadoc")
            from(tasks["javadoc"])
        }
        tasks.withType(Javadoc::class) {
            isFailOnError = false
        }
    }
    plugins.withId("maven-publish") {
        println("- Configuring `publishing`")

        afterEvaluate {
            extensions.configure<PublishingExtension> {
                publications {
                    withType(MavenPublication::class) {
                        version = project.version.toString()
                        group = project.group.toString()

                        pom {
                            name.set("MaterialThemeBuilder")
                            description.set("A gradle plugin that generates Material Design 3 themes for Android projects.")
                            url.set("https://github.com/ClautoW/MaterialThemeBuilder/")
                            licenses {
                                license {
                                    name.set("MIT License")
                                    url.set("https://raw.githubusercontent.com/ClautoW/MaterialThemeBuilder/master/LICENSE")
                                }
                            }
                            developers {
                                developer {
                                    name.set("ClautoW")
                                }
                            }
                            scm {
                                connection.set("scm:git:https://github.com/ClautoW/MaterialThemeBuilder.git")
                                url.set("https://github.com/ClautoW/MaterialThemeBuilder/")
                            }
                        }
                    }
                }
                repositories {
                    mavenLocal()
                    maven {
                        name = "ossrh"
                        url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
                        credentials(PasswordCredentials::class.java)
                    }
                    maven {
                        name = "github"
                        url = uri("https://maven.pkg.github.com/cl-andro/clauto-material-theme")
                        credentials {
                            username = System.getenv("GITHUB_ACTOR")
                            password = System.getenv("GITHUB_TOKEN")
                        }
                    }
                }
            }
        }
        plugins.withId("signing") {
            println("- Configuring `signing`")

            afterEvaluate {
                extensions.configure<SigningExtension> {
                    val signingKey = findProperty("signingKey") as? String
                    val signingPassword = findProperty("signingPassword") as? String
                    val secretKeyRingFile = findProperty("signing.secretKeyRingFile") as? String
                    val gnuPgDir = java.io.File(System.getProperty("user.home") + "/.gnupg/private-keys-v1.d")

                    val hasGpg = gnuPgDir.isDirectory && (gnuPgDir.list()?.size ?: 0) > 0
                    val hasKeyRing = secretKeyRingFile != null && java.io.File(secretKeyRingFile).exists()
                    val hasInMemory = !signingKey.isNullOrEmpty()

                    if (hasGpg || hasKeyRing || hasInMemory) {
                        if (hasGpg) {
                            useGpgCmd()
                        } else if (hasInMemory) {
                            useInMemoryPgpKeys(signingKey, signingPassword)
                        }

                        val signingTasks = sign(extensions.getByType<PublishingExtension>().publications)
                        tasks.withType(AbstractPublishToMaven::class) {
                            dependsOn(signingTasks)
                        }
                    }
                }
            }
        }
    }
}
