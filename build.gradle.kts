plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.spotless)
    alias(libs.plugins.cyclonedx.bom)
    alias(libs.plugins.spdx.sbom)
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

    // Force Netty HTTP/2 to patched version to fix security vulnerability GHSA-prj3-ccx8-p6x4
    configurations.configureEach {
        resolutionStrategy {
            force(
                libs.netty.codec.http2
                    .get()
                    .toString(),
            )
        }
    }
}

subprojects {
    // Don't automatically apply Kotlin plugin to avoid circular dependencies
    // Each project should apply it explicitly

    // Configure Kotlin compilation when plugin is applied
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            }
        }
    }

    // Fix circular dependency issue with Kotlin and Java compilation
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    // Prevent dynamic versions to improve build reproducibility
    configurations.all {
        resolutionStrategy {
            preferProjectModules()
            // Force Netty to patched version to fix GHSA-prj3-ccx8-p6x4 vulnerability
            force(
                libs.netty.codec.http2
                    .get()
                    .toString(),
            )
        }
    }
}

// Configure detekt for projects that have the plugin applied
subprojects {
    pluginManager.withPlugin("io.gitlab.arturbosch.detekt") {
        configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            config.setFrom("$rootDir/detekt.yml")
            buildUponDefaultConfig = true
            allRules = false
            parallel = true
            baseline = file("detekt-baseline.xml")
        }
    }
}

ktlint {
    version.set(
        libs.versions.ktlint.tool
            .get(),
    )
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
    dependsOn(":quality-konsist:test")
}

// Spotless configuration
configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**/*.kt", "**/.tmp/**/*.kt", "**/.gradle-local/**/*.kt")
        ktlint(
            libs.versions.ktlint.tool
                .get(),
        ).editorConfigOverride(
            mapOf(
                "indent_size" to 4,
                "continuation_indent_size" to 4,
                "max_line_length" to 160,
                "insert_final_newline" to true,
                "ktlint_standard_no-wildcard-imports" to "disabled",
                "ktlint_standard_package-name" to "disabled",
                "ktlint_standard_value-parameter-comment" to "disabled",
            ),
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/.gradle-local/**/*.gradle.kts")
        ktlint(
            libs.versions.ktlint.tool
                .get(),
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
    json {
        target("**/*.json")
        targetExclude("**/build/**/*.json", "**/node_modules/**/*.json", "**/.gradle-local/**/*.json", "**/package.json")
        jackson()
        trimTrailingWhitespace()
        endWithNewline()
    }
    yaml {
        target("**/*.{yml,yaml}")
        targetExclude("**/build/**/*.{yml,yaml}", "**/.gradle-local/**/*.{yml,yaml}")
        jackson()
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("markdown") {
        target("**/*.md")
        targetExclude("**/build/**/*.md", "**/.gradle-local/**/*.md")
        endWithNewline()
        // Trailing whitespace has semantic meaning in Markdown, so follow .editorconfig
    }
    format("shell") {
        target("**/*.sh")
        targetExclude("**/build/**/*.sh")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
