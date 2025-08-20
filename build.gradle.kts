plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.graalvm.native) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint)
    id("com.diffplug.spotless") version "7.2.1"
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

    // Prevent dynamic versions to improve build reproducibility
    configurations.all {
        resolutionStrategy {
            preferProjectModules()
        }
    }
}

// Configure detekt for all subprojects
subprojects {
    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom("$rootDir/detekt.yml")
        buildUponDefaultConfig = true
        allRules = false
        parallel = true
        baseline = file("detekt-baseline.xml")
    }
}

// Custom task to check if GraalVM is available
tasks.register("checkGraalVM") {
    doLast {
        try {
            val isWindows = System.getProperty("os.name").lowercase().contains("windows")
            val nativeImageExecutable = if (isWindows) "native-image.cmd" else "native-image"
            val javaHome = System.getProperty("java.home")
            val nativeImagePath = File("$javaHome/bin/$nativeImageExecutable")

            if (!nativeImagePath.exists()) {
                // Try alternative paths for different GraalVM installations
                val altPaths =
                    listOf(
                        "$javaHome/../bin/$nativeImageExecutable",
                        "$javaHome/bin/$nativeImageExecutable",
                    )

                val foundPath = altPaths.find { File(it).exists() }
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

// Spotless configuration
spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", "**/generated/**", ".gradle/**")
        ktlint("1.5.0")
            .editorConfigOverride(
                mapOf(
                    "indent_size" to "4",
                    "continuation_indent_size" to "4",
                    "max_line_length" to "150",
                    "insert_final_newline" to "true",
                    "ktlint_standard_no-wildcard-imports" to "disabled",
                    "ktlint_standard_package-name" to "disabled",
                    "ktlint_standard_filename" to "disabled",
                    "ktlint_standard_function-naming" to "disabled",
                    "ktlint_standard_value-parameter-comment" to "disabled",
                    "ktlint_standard_max-line-length" to "disabled",
                ),
            )
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**", ".gradle/**")
        ktlint("1.5.0")
        trimTrailingWhitespace()
        endWithNewline()
    }

    format("misc") {
        target("**/*.md", "**/*.yml", "**/*.yaml", "**/*.json")
        targetExclude("**/build/**", ".gradle/**", "**/node_modules/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// CycloneDX SBOM Configuration
tasks.cyclonedxBom {
    setDestination(project.file("build/reports"))
    setOutputName("bom")
    setOutputFormat("json")
}

// Task to run architecture tests
tasks.register("konsistTest") {
    description = "Run Konsist architecture tests"
    group = "verification"
    dependsOn(":konsist-test:test")
}
