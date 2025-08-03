plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("org.graalvm.buildtools.native") version "0.11.0"
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0"
    application
}

group = "com.kamiazya.scopes"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.github.ajalt.clikt:clikt:4.4.0")

    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.kotest:kotest-property:5.9.1")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

application {
    mainClass.set("com.kamiazya.scopes.MainKt")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("scopes")
            mainClass.set("com.kamiazya.scopes.MainKt")
            useFatJar.set(true)

            buildArgs.addAll(
                "--no-fallback",
                "--enable-http",
                "--enable-url-protocols=https",
                "--report-unsupported-elements-at-runtime",
                "-H:+ReportExceptionStackTraces",
                "-H:IncludeResources=.*",
                "-H:+InstallExitHandlers",
                "--initialize-at-build-time=kotlin",
                "--initialize-at-build-time=kotlinx.coroutines",
                "--initialize-at-run-time=io.netty",
            )
        }
    }

    toolchainDetection.set(false)
}

ktlint {
    version.set("1.5.0")
    outputToConsole.set(true)
    coloredOutput.set(true)
    verbose.set(true)
    android.set(false)
    ignoreFailures.set(false)

    filter {
        exclude("**/generated/**")
        include("src/**/*.kt")
    }
}
