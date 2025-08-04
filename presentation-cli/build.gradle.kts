plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.graalvm.native)
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

            buildArgs.addAll(
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
        }
    }

    toolchainDetection.set(false)
}

// Make nativeCompile depend on checkGraalVM
tasks.named("nativeCompile") {
    dependsOn(":checkGraalVM")
}
