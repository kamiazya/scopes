plugins {
    kotlin("jvm")
    id("org.graalvm.buildtools.native")
    application
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation(project(":infrastructure"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.github.ajalt.clikt:clikt:${project.ext["clikt"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${project.ext["kotlinCoroutines"]}")

    testImplementation("io.kotest:kotest-runner-junit5:${project.ext["kotest"]}")
    testImplementation("io.kotest:kotest-assertions-core:${project.ext["kotest"]}")
    testImplementation("io.kotest:kotest-property:${project.ext["kotest"]}")
    testImplementation("io.mockk:mockk:${project.ext["mockk"]}")
}

application {
    mainClass.set("com.kamiazya.scopes.presentation.cli.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("scopes")
            mainClass.set("com.kamiazya.scopes.presentation.cli.MainKt")
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
            )
        }
    }

    toolchainDetection.set(false)
}

// Make nativeCompile depend on checkGraalVM
tasks.named("nativeCompile") {
    dependsOn(":checkGraalVM")
}
