plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.graalvm.native)
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

graalvmNative {
    binaries {
        named("main") {
            imageName.set("scopesd")
            mainClass.set("io.github.kamiazya.scopes.apps.daemon.MainKt")
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
                    "-Djdk.util.jar.enableMultiRelease=force",
                    "--initialize-at-build-time=kotlin",
                    "--initialize-at-build-time=kotlinx.coroutines",
                    "--initialize-at-run-time=kotlin.uuid.SecureRandomHolder",
                    "--initialize-at-run-time=org.sqlite",
                    "-Dorg.sqlite.lib.exportPath=${layout.buildDirectory.get()}/native/nativeCompile",
                    "--exclude-config",
                    ".*sqlite-jdbc.*\\.jar",
                    ".*native-image.*",
                    "-H:ResourceConfigurationFiles=${layout.buildDirectory.get()}/resources/main/META-INF/native-image/resource-config.json",
                    "-H:ReflectionConfigurationFiles=${layout.buildDirectory.get()}/resources/main/META-INF/native-image/reflect-config.json",
                    "-H:JNIConfigurationFiles=${layout.buildDirectory.get()}/resources/main/META-INF/native-image/jni-config.json",
                    // Additional settings for gRPC
                    "--initialize-at-run-time=io.grpc.netty.shaded.io.netty",
                    "--initialize-at-run-time=io.grpc.netty.shaded.io.netty.channel.epoll.Epoll",
                    "--initialize-at-run-time=io.grpc.netty.shaded.io.netty.channel.epoll.Native",
                    "--initialize-at-run-time=io.grpc.netty.shaded.io.netty.channel.epoll.EpollEventLoop",
                    "--initialize-at-run-time=io.grpc.netty.shaded.io.netty.channel.DefaultFileRegion",
                    "--initialize-at-run-time=io.grpc.netty.shaded.io.netty.channel.kqueue.KQueueEventArray",
                    "--initialize-at-run-time=io.grpc.netty.shaded.io.netty.channel.kqueue.KQueueEventLoop",
                    "--initialize-at-run-time=io.grpc.netty.shaded.io.netty.channel.kqueue.Native",
                    "--initialize-at-run-time=io.grpc.netty.shaded.io.netty.channel.unix.Errors",
                    "--initialize-at-run-time=io.grpc.netty.shaded.io.netty.channel.unix.IovArray",
                    "--initialize-at-run-time=io.grpc.netty.shaded.io.netty.channel.unix.Limits",
                    "--enable-url-protocols=http,https"
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
