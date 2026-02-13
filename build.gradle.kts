import java.security.MessageDigest

plugins {
    java
    `maven-publish`
    signing
}

group = property("group") as String
version = property("version") as String

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

val log4jVersion = "2.25.3"

dependencies {
    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    // HTTP
    implementation("org.apache.httpcomponents:httpclient:4.5.14") {
        exclude(group = "commons-codec", module = "commons-codec")
        exclude(group = "commons-logging", module = "commons-logging")
    }
    implementation("commons-codec:commons-codec:1.20.0")

    // JSON / Data
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.1")
    implementation("org.yaml:snakeyaml:2.5")

    // Logging
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-jcl:$log4jVersion")

    // CLI
    implementation("com.beust:jcommander:1.82")

    // Commons
    implementation("commons-io:commons-io:2.21.0")
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("org.apache.commons:commons-lang3:3.19.0")

    // System info
    implementation("com.github.oshi:oshi-json:3.13.6")

    // SSH
    implementation("com.github.mwiede:jsch:2.27.6")

    // Versioning
    implementation("org.semver4j:semver4j:6.0.0")

    // Text IO
    implementation("org.beryx:text-io:3.4.1") {
        exclude(group = "org.slf4j", module = "slf4j-api")
        exclude(group = "jline", module = "jline")
    }
    implementation("jline:jline:2.14.6")

    // Templating
    implementation("org.freemarker:freemarker:2.3.34")

    // JAXB (needed since removal from JDK 9+)
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.6")

    // Test
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.14.1")
    testImplementation("org.junit.vintage:junit-vintage-engine:5.14.1")
    testImplementation("org.junit.platform:junit-platform-launcher:1.14.1")
    testRuntimeOnly("org.junit.platform:junit-platform-surefire-provider:1.3.2")
    testImplementation("org.mock-server:mockserver-netty:5.15.0")
}

// ---------------------------------------------------------------------------
// Jar
// ---------------------------------------------------------------------------
tasks.jar {
    exclude("**/*.xml", "**/*.json", "**/*.ftlh")
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
        )
    }
}

// ---------------------------------------------------------------------------
// Copy runtime dependencies to build/lib (used by Docker builds)
// ---------------------------------------------------------------------------
val copyDependencies by tasks.registering(Copy::class) {
    group = "distribution"
    description = "Copies runtime dependency jars to build/lib."
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("lib"))
}

// ---------------------------------------------------------------------------
// Distribution zip (mirrors src/main/assembly/assembly.xml)
// ---------------------------------------------------------------------------
val distZip by tasks.registering(Zip::class) {
    group = "distribution"
    description = "Builds the distribution zip matching the Maven assembly layout."

    archiveBaseName.set("diagnostics")
    archiveClassifier.set("dist")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    dependsOn(tasks.jar, configurations.runtimeClasspath)

    // scripts/** → root (excluding share_ad_job_state/tests/**)
    from("scripts") {
        exclude("share_ad_job_state/tests/**")
    }

    // Root docs
    from(".") {
        include("LICENSE.txt", "NOTICE.txt", "README.md")
    }

    // config/
    from("src/main/resources") {
        into("config")
    }

    // lib/ — project jar + runtime dependencies
    into("lib") {
        from(tasks.jar)
        from(configurations.runtimeClasspath)
    }
}

// ---------------------------------------------------------------------------
// SHA-256 checksum for the dist zip
// ---------------------------------------------------------------------------
val distZipChecksum by tasks.registering {
    group = "distribution"
    description = "Generates a SHA-256 checksum for the distribution zip."
    dependsOn(distZip)

    val zipFile = distZip.flatMap { it.archiveFile }
    val checksumFile = zipFile.map { file("${it.asFile.absolutePath}.sha256") }

    inputs.file(zipFile)
    outputs.file(checksumFile)

    doLast {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = zipFile.get().asFile.readBytes()
        val hash = digest.digest(bytes)
        val hex = hash.joinToString("") { byte -> "%02x".format(byte) }
        checksumFile.get().writeText("$hex  ${zipFile.get().asFile.name}\n")
    }
}

// Wire distribution tasks into the build lifecycle
tasks.named("build") {
    dependsOn(distZip, distZipChecksum, copyDependencies)
}

// ---------------------------------------------------------------------------
// Test
// ---------------------------------------------------------------------------
tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    options.isFork = true
    options.encoding = "UTF-8"
}

// ---------------------------------------------------------------------------
// Publishing to Maven Central (OSSRH)
// ---------------------------------------------------------------------------
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("Support Diagnostics Utilities")
                description.set("Elastic Support Diagnostics Utilities")
                url.set("http://github.com/elastic/support-diagnostics")
                inceptionYear.set("2014")

                licenses {
                    license {
                        name.set("Elastic License")
                        url.set("https://www.elastic.co/licensing/elastic-license")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        organization.set("Elastic")
                        organizationUrl.set("https://www.elastic.co")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/elastic/support-diagnostics.git")
                    developerConnection.set("scm:git:ssh://github.com:elastic/support-diagnostics.git")
                    url.set("http://github.com/elastic/support-diagnostics/tree/main")
                }
            }
        }
    }

    repositories {
        maven {
            name = "ossrhSnapshots"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
            credentials {
                username = findProperty("ossrhUsername") as String?
                    ?: System.getenv("OSSRH_USERNAME")
                password = findProperty("ossrhPassword") as String?
                    ?: System.getenv("OSSRH_PASSWORD")
            }
        }
        maven {
            name = "ossrhStaging"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = findProperty("ossrhUsername") as String?
                    ?: System.getenv("OSSRH_USERNAME")
                password = findProperty("ossrhPassword") as String?
                    ?: System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    // Use GPG command-line tool when a key is configured
    if ((findProperty("signing.gnupg.keyName") ?: System.getenv("GPG_KEY_ID")) != null) {
        useGpgCmd()
    }

    sign(publishing.publications["mavenJava"])
    isRequired = gradle.taskGraph.hasTask("publish")
}
