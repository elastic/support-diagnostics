import java.security.MessageDigest

plugins {
    java
    `maven-publish`
    signing
    id("com.github.jk1.dependency-license-report")
}

group = property("group") as String
version = property("version") as String

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

val log4jVersion = "2.26.0"

dependencies {
    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    // HTTP
    implementation("org.apache.httpcomponents:httpclient:4.5.14") {
        exclude(group = "commons-codec", module = "commons-codec")
        exclude(group = "commons-logging", module = "commons-logging")
    }
    implementation("commons-codec:commons-codec:1.22.0")

    // JSON / Data
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.3")
    implementation("org.yaml:snakeyaml:2.6")

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
    implementation("org.apache.commons:commons-lang3:3.20.0")

    // System info
    implementation("com.github.oshi:oshi-json:3.13.6")

    // SSH
    implementation("com.github.mwiede:jsch:2.28.0")

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

    // Test
    testImplementation("org.junit.jupiter:junit-jupiter-engine:6.0.3")
    testImplementation("org.junit.platform:junit-platform-launcher:6.0.3")
    testImplementation("org.wiremock:wiremock:3.13.2")

    // Testcontainers
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.21.3"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:testcontainers")
    // SLF4J 2.x binding so Testcontainers debug logs are visible (log4j-slf4j-impl targets 1.x)
    testImplementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")
}

// ---------------------------------------------------------------------------
// Generate NOTICE.txt from NOTICE.template + runtime dependency licenses
// ---------------------------------------------------------------------------
licenseReport {
    configurations = arrayOf("runtimeClasspath")
    renderers = arrayOf(co.elastic.support.gradle.NoticeRenderer("NOTICE.template", "NOTICE.txt"))
    excludeOwnGroup = true
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
    jvmArgs("-Djava.net.preferIPv4Stack=true", "-Djava.security.egd=file:/dev/./urandom")
    maxHeapSize = "512m"
}

tasks.named<Test>("test") {
    useJUnitPlatform { excludeTags("e2e") }
}

tasks.register<Test>("e2eTest") {
    description = "Runs end-to-end tests that require Docker (via Testcontainers)"
    group = "verification"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    useJUnitPlatform { includeTags("e2e") }

    // does not require running 'test', but if both run, then e2eTest runs second
    shouldRunAfter(tasks["test"])

    // passthru environment variables to help CI if needed, otherwise use code-level defaults
    for (name in listOf("E2E_STARTUP_TIMEOUT_MINUTES", "ELASTIC_STACK_VERSION")) {
        System.getenv(name)?.let { environment(name, it) }
    }

    maxHeapSize = "1g"

    jvmArgs("-Djava.net.preferIPv4Stack=true", "-Djava.security.egd=file:/dev/./urandom")
    // docker-java reads API version from the "api.version" system property (not env var).
    // Docker Desktop 4.71+ requires >= 1.40; docker-java defaults to 1.32 without this.
    jvmArgs("-Dapi.version=1.40")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    options.isFork = true // fork to guarantee that Lombok does not mess with the Gradle JVM
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
