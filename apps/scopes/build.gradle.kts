import java.time.Instant

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    alias(libs.plugins.cyclonedx.bom)
    alias(libs.plugins.spdx.sbom)
    application
}

dependencies {
    // Platform layer
    implementation(project(":platform-commons"))
    implementation(project(":platform-domain-commons"))
    implementation(project(":platform-observability"))
    implementation(project(":platform-application-commons"))
    implementation(project(":platform-infrastructure"))

    // Contracts layer
    implementation(project(":contracts-scope-management"))
    implementation(project(":contracts-user-preferences"))

    // Interface layer
    implementation(project(":interfaces-cli"))
    implementation(project(":interfaces-mcp"))

    // gRPC Client for daemon communication
    implementation(project(":interfaces-grpc-client-daemon"))
    implementation(project(":interfaces-rpc-contracts"))

    // TODO: Enable when implemented
    // implementation(project(":contexts:aspect-management:application"))
    // implementation(project(":contexts:alias-management:application"))
    // implementation(project(":contexts:context-management:application"))

    // CLI framework
    implementation(libs.clikt)

    // Core libraries
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.io.core)
    implementation(libs.arrow.core)

    // DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    // Logging
    runtimeOnly(libs.logback.classic)

    // Testing
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

application {
    mainClass.set("io.github.kamiazya.scopes.apps.cli.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

// Configure JAR task to create an executable JAR with dependencies
tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "io.github.kamiazya.scopes.apps.cli.MainKt",
            "Implementation-Version" to project.version,
            "Implementation-Title" to "Scopes CLI"
        )
    }
}

// Create a fat JAR with all dependencies
tasks.register<Jar>("fatJar") {
    dependsOn(tasks.classes)

    archiveBaseName.set("scopes")
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes(
            "Main-Class" to "io.github.kamiazya.scopes.apps.cli.MainKt",
            "Implementation-Version" to project.version,
            "Implementation-Title" to "Scopes CLI"
        )
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    }) {
        exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
        exclude("META-INF/MANIFEST.MF")
        exclude("META-INF/versions/**")
        exclude("module-info.class")
    }
}

// Generate version.txt from project version
tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("version.txt") {
        expand("version" to project.version)
    }

    // Also generate build-info.properties for more detailed information
    doLast {
        val buildInfoFile = file("$destinationDir/build-info.properties")
        buildInfoFile.parentFile.mkdirs()

        // Get git revision if available
        val gitRevision =
            try {
                val process =
                    ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                        .directory(project.rootDir)
                        .start()
                process.inputStream.bufferedReader().use { it.readText().trim() }
            } catch (e: Exception) {
                logger.warn("Failed to get git revision: ${e.message}")
                "unknown"
            }

        // Check if working directory is dirty
        val isDirty =
            try {
                val process =
                    ProcessBuilder("git", "status", "--porcelain")
                        .directory(project.rootDir)
                        .start()
                val output = process.inputStream.bufferedReader().use { it.readText().trim() }
                output.isNotEmpty()
            } catch (e: Exception) {
                logger.warn("Failed to check git status: ${e.message}")
                false
            }

        val gitRevisionWithStatus =
            if (isDirty && gitRevision != "unknown") {
                "$gitRevision-dirty"
            } else {
                gitRevision
            }

        buildInfoFile.writeText(
            """
            version=${project.version}
            build.time=${Instant.now()}
            git.revision=$gitRevisionWithStatus
            gradle.version=${gradle.gradleVersion}
            java.version=${System.getProperty("java.version")}
            java.vendor=${System.getProperty("java.vendor")}
            os.name=${System.getProperty("os.name")}
            os.arch=${System.getProperty("os.arch")}
            """.trimIndent(),
        )
    }
}

