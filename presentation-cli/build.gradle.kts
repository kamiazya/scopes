plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.graalvm.native)
    id("org.cyclonedx.bom") version "2.3.1"
    id("org.spdx.sbom") version "0.9.0"
    application
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation(project(":infrastructure"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.clikt)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.arrow.core)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

application {
    mainClass.set("io.github.kamiazya.scopes.presentation.cli.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("scopes")
            mainClass.set("io.github.kamiazya.scopes.presentation.cli.MainKt")
            useFatJar.set(true)

            val commonArgs = listOf(
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
            )

            val os = System.getProperty("os.name").lowercase()
            val isWindows = os.contains("windows") ||
                System.getenv("RUNNER_OS") == "Windows" ||
                (System.getenv("OS")?.lowercase()?.contains("windows") == true)
            val isLinux = os.contains("linux") ||
                System.getenv("RUNNER_OS") == "Linux" ||
                (System.getenv("OS")?.lowercase()?.contains("linux") == true)

            // Only add minimal, non-duplicated platform specifics.
            val platformSpecificArgs = if (isWindows) {
                listOf(
                    "-H:+AllowIncompleteClasspath",
                    "-H:DeadlockWatchdogInterval=0"
                )
            } else if (isLinux) {
                listOf(
                    // On Linux, allow mostly-static linking (libc dynamically).
                    "-H:+StaticExecutableWithDynamicLibC"
                )
            } else {
                emptyList()
            }

            buildArgs.addAll(commonArgs + platformSpecificArgs)
        }
    }

    toolchainDetection.set(false)
}
// Windows-specific JAR handling to avoid module-info conflicts
tasks.named("nativeCompileClasspathJar") {
    doFirst {
        if (System.getenv("RUNNER_OS") == "Windows") {
            println("Windows detected: Configuring JAR to handle module-info conflicts")
        }
    }
}

// Make nativeCompile depend on checkGraalVM
tasks.named("nativeCompile") {
    dependsOn(":checkGraalVM")
    // Disable configuration cache for this task due to GraalVM plugin compatibility issues
    // See: https://github.com/graalvm/native-build-tools/issues/477
    notCompatibleWithConfigurationCache("GraalVM plugin uses ConfigurationContainer at execution time")
}

// Disable configuration cache for GraalVM-related tasks
tasks.withType<org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask> {
    notCompatibleWithConfigurationCache("GraalVM plugin compatibility issue")
}

tasks.withType<org.graalvm.buildtools.gradle.tasks.NativeRunTask> {
    notCompatibleWithConfigurationCache("GraalVM plugin compatibility issue")
}

tasks.matching { it.name == "generateResourcesConfigFile" }.configureEach {
    notCompatibleWithConfigurationCache("GraalVM plugin compatibility issue")
}

// CycloneDX SBOM Configuration
tasks.cyclonedxBom {
    setDestination(project.file("build/reports"))
    setOutputName("bom")
    setOutputFormat("json")
}

