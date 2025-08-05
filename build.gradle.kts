plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.graalvm.native) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    id("org.cyclonedx.bom") version "2.3.1"
    id("org.spdx.sbom") version "0.9.0"
}

group = "io.github.kamiazya"
version = project.findProperty("version")?.toString() ?: "0.0.0-SNAPSHOT"

allprojects {
    group = "io.github.kamiazya"
    version = project.findProperty("version")?.toString() ?: "0.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    // Configure detekt for each subproject
    detekt {
        config.setFrom("$rootDir/detekt.yml")
        buildUponDefaultConfig = true
        allRules = false
        parallel = true
    }
}

// Custom task to check if GraalVM is available
tasks.register("checkGraalVM") {
    doLast {
        try {
            val isWindows = System.getProperty("os.name").lowercase().contains("windows")
            val nativeImageExecutable = if (isWindows) "native-image.cmd" else "native-image"
            val nativeImagePath = file("${System.getProperty("java.home")}/bin/$nativeImageExecutable")

            if (!nativeImagePath.exists()) {
                // Try alternative paths for different GraalVM installations
                val altPaths =
                    listOf(
                        "${System.getProperty("java.home")}/../bin/$nativeImageExecutable",
                        "${System.getProperty("java.home")}/bin/$nativeImageExecutable",
                    )

                val foundPath = altPaths.find { file(it).exists() }
                if (foundPath == null) {
                    println("⚠️ GraalVM native-image not found in expected locations")
                    println("This is expected in CI environments where GraalVM is set up dynamically")
                    println("Skipping native-image availability check")
                    return@doLast
                } else {
                    println("✅ GraalVM native-image found at: $foundPath")
                }
            } else {
                println("✅ GraalVM native-image found at: $nativeImagePath")
            }
        } catch (e: Exception) {
            println("⚠️ Cannot verify GraalVM native-image availability: ${e.message}")
            println("This may be normal in CI environments - continuing with build")
        }
    }
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
        include("**/src/**/*.kt")
        exclude("**/build/**")
    }
}

// SBOM Configuration will be added to subprojects that need it
