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
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.github.ajalt.clikt:clikt:4.4.0")
    implementation("com.github.guepardoapps:kulid:2.0.0.0")

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
                "-Os",
                "--no-fallback",
                "--gc=serial",
                "--enable-http",
                "--enable-url-protocols=https",
                "--report-unsupported-elements-at-runtime",
                "-H:+ReportExceptionStackTraces",
                "-H:+StripDebugInfo",
                "-H:+ReduceImplicitExceptionStackTraceInformation",
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

// Custom task to check if GraalVM is available
tasks.register("checkGraalVM") {
    doLast {
        try {
            val nativeImagePath = file("${System.getProperty("java.home")}/bin/native-image")
            if (!nativeImagePath.exists()) {
                throw GradleException("GraalVM with native-image is not installed. Please install GraalVM or run CI tests.")
            }
            println("✅ GraalVM native-image found at: $nativeImagePath")
        } catch (e: Exception) {
            throw GradleException("❌ GraalVM native-image not found. Install GraalVM for local native compilation.")
        }
    }
}

// Make nativeCompile depend on checkGraalVM
tasks.named("nativeCompile") {
    dependsOn("checkGraalVM")
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
