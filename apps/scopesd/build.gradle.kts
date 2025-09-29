plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    // Koin BOM
    implementation(platform(libs.koin.bom))

    // Platform layer
    implementation(project(":platform-commons"))
    implementation(project(":platform-observability"))
    implementation(project(":platform-application-commons"))
    implementation(project(":platform-domain-commons"))
    implementation(project(":platform-infrastructure"))

    // Bounded Context - Unified Scope Management (Full stack for daemon)
    implementation(project(":scope-management-domain"))
    implementation(project(":scope-management-application"))
    implementation(project(":scope-management-infrastructure"))

    // Contract ports
    implementation(project(":contracts-scope-management"))

    // gRPC daemon interface
    implementation(project(":interfaces-grpc-server-daemon"))

    // Core libraries
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.serialization.json)

    // DI
    implementation(libs.koin.core)

    // Logging
    runtimeOnly(libs.logback.classic)

    // Testing
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

application {
    mainClass.set("io.github.kamiazya.scopes.apps.daemon.MainKt")
}

tasks.test {
    useJUnitPlatform()
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
            build.time=${System.currentTimeMillis()}
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

// Configure JAR task to create an executable JAR with dependencies
tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "io.github.kamiazya.scopes.apps.daemon.MainKt",
            "Implementation-Version" to project.version,
            "Implementation-Title" to "Scopes Daemon"
        )
    }
}

// Create a fat JAR with all dependencies
tasks.register<Jar>("fatJar") {
    dependsOn(tasks.classes)

    archiveBaseName.set("scopesd")
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes(
            "Main-Class" to "io.github.kamiazya.scopes.apps.daemon.MainKt",
            "Implementation-Version" to project.version,
            "Implementation-Title" to "Scopes Daemon"
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
