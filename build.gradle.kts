plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.graalvm.native) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

group = "io.github.kamiazya"
version = "0.0.1"

allprojects {
    group = "io.github.kamiazya"
    version = "0.0.1"

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
    }
}

detekt {
    source.setFrom("src/main/kotlin")
    config.setFrom("$rootDir/detekt.yml")
    buildUponDefaultConfig = true
    allRules = false
    parallel = true
}
