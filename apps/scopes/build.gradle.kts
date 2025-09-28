import java.time.Instant

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    alias(libs.plugins.graalvm.native)
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

    // GraalVM native image
    compileOnly(libs.graalvm.sdk)

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

graalvmNative {
    binaries {
        named("main") {
            imageName.set("scopes")
            mainClass.set("io.github.kamiazya.scopes.apps.cli.MainKt")
            useFatJar.set(true)

            buildArgs.addAll(
                listOf(
                    "-O2",
                    "--no-fallback",
                    "--gc=serial",
                    "--report-unsupported-elements-at-runtime",
                    "-H:+UnlockExperimentalVMOptions",
                    "-H:+ReportExceptionStackTraces",
                    "-H:+InstallExitHandlers",
                    "--initialize-at-build-time=kotlin",
                    "--initialize-at-build-time=kotlinx.coroutines",
                    "--initialize-at-run-time=kotlin.uuid.SecureRandomHolder",
                    "-Dio.netty.leakDetection.level=disabled",
                    "-Dio.netty.noResourceLeakDetection=true",
                    "-H:ResourceConfigurationFiles=${layout.buildDirectory.get()}/resources/main/META-INF/native-image/resource-config.json",
                    "-H:ReflectionConfigurationFiles=${layout.buildDirectory.get()}/resources/main/META-INF/native-image/reflect-config.json",
                    "-H:JNIConfigurationFiles=${layout.buildDirectory.get()}/resources/main/META-INF/native-image/jni-config.json",
                ),
            )
        }
    }

    toolchainDetection.set(false)
}

// Make nativeCompile depend on checkGraalVM
tasks.named("nativeCompile") {
    dependsOn(":checkGraalVM")
}

// E2E Test Tasks for Native Binary

// Common function to get native binary path
fun getNativeBinaryPath(): File {
    val os =
        org.gradle.internal.os.OperatingSystem
            .current()
    val binaryName = if (os.isWindows) "scopes.exe" else "scopes"
    return layout.buildDirectory
        .file("native/nativeCompile/$binaryName")
        .get()
        .asFile
}

// Smoke test - quick verification that binary can run
// NOTE: Does not depend on nativeCompile to avoid rebuilding with different flags
// tasks.register<Exec>("nativeSmokeTest") {
//     group = "verification"
//     description = "Run basic smoke test on native binary"

//     val binaryPath = getNativeBinaryPath()

//     doFirst {
//         if (!binaryPath.exists()) {
//             throw GradleException("Native binary not found at: ${binaryPath.absolutePath}")
//         }
//         val os =
//             org.gradle.internal.os.OperatingSystem
//                 .current()
//         if (!os.isWindows && !binaryPath.canExecute()) {
//             binaryPath.setExecutable(true)
//         }
//         logger.lifecycle("Running smoke test on: ${binaryPath.absolutePath}")
//     }

//     // Test --help flag
//     commandLine(binaryPath.absolutePath, "--help")

//     doLast {
//         logger.lifecycle("✅ Smoke test passed: binary is executable")
//     }
// }

// Full E2E test suite
tasks.register("nativeE2eTest") {
    group = "verification"
    description = "Run full E2E test suite on native binary"
    // dependsOn("nativeSmokeTest")

    doLast {
        val binaryPath = getNativeBinaryPath()

        if (!binaryPath.exists()) {
            throw GradleException("Native binary not found at: ${binaryPath.absolutePath}")
        }

        logger.lifecycle("Running E2E tests on native binary...")

        // Test basic commands
        val testCases =
            listOf(
                listOf("--help"),
                listOf("scope", "--help"),
                listOf("context", "--help"),
                listOf("workspace", "--help"),
            )

        testCases.forEach { args ->
            try {
                project.exec {
                    commandLine(listOf(binaryPath.absolutePath) + args)
                    standardOutput = System.out
                    errorOutput = System.err
                }
                logger.lifecycle("✅ Test passed: ${args.joinToString(" ")}")
            } catch (e: Exception) {
                throw GradleException("❌ Test failed for: ${args.joinToString(" ")}\n${e.message}")
            }
        }

        logger.lifecycle("✅ All E2E tests passed successfully")
    }
}

// Add smoke test to check task
// tasks.named("check") {
//     dependsOn("nativeSmokeTest")
// }
