plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    alias(libs.plugins.graalvm.native)
    id("org.cyclonedx.bom") version "2.3.1"
    id("org.spdx.sbom") version "0.9.0"
    application
}

dependencies {
    // Platform layer
    implementation(project(":platform:commons"))
    implementation(project(":platform:observability"))
    implementation(project(":platform:application-commons"))

    // Interface layer
    implementation(project(":interfaces:shared"))
    implementation(project(":interfaces:cli"))

    // Bounded Contexts - scope-management
    implementation(project(":contexts:scope-management:domain"))
    implementation(project(":contexts:scope-management:application"))
    implementation(project(":contexts:scope-management:infrastructure"))

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
    implementation(libs.arrow.core)

    // DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    // Testing
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

application {
    mainClass.set("io.github.kamiazya.scopes.apps.cli.SimpleScopesCommandKt")
}

tasks.test {
    useJUnitPlatform()
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("scopes")
            mainClass.set("io.github.kamiazya.scopes.apps.cli.SimpleScopesCommandKt")
            useFatJar.set(true)

            val commonArgs =
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
                )

            val os = System.getProperty("os.name").lowercase()
            val isWindows =
                os.contains("windows") ||
                    System.getenv("RUNNER_OS") == "Windows" ||
                    (System.getenv("OS")?.lowercase()?.contains("windows") == true)
            val isLinux =
                os.contains("linux") ||
                    System.getenv("RUNNER_OS") == "Linux" ||
                    (System.getenv("OS")?.lowercase()?.contains("linux") == true)

            // Only add minimal, non-duplicated platform specifics.
            val platformSpecificArgs =
                if (isWindows) {
                    listOf(
                        "-H:+AllowIncompleteClasspath",
                        "-H:DeadlockWatchdogInterval=0",
                    )
                } else if (isLinux) {
                    listOf(
                        // On Linux, allow mostly-static linking (libc dynamically).
                        "-H:+StaticExecutableWithDynamicLibC",
                    )
                } else {
                    emptyList()
                }

            buildArgs.addAll(commonArgs + platformSpecificArgs)
        }
    }

    toolchainDetection.set(false)
}

// Make nativeCompile depend on checkGraalVM
tasks.named("nativeCompile") {
    dependsOn(":checkGraalVM")
}
