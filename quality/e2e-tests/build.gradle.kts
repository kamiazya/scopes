@file:Suppress("UnstableApiUsage")

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.kotlin.jvm)
}

val isCiEnvironment = System.getenv("CI")?.toBoolean() == true

dependencies {
    implementation(project(":platform-commons"))
    implementation(project(":platform-observability"))
    
    // Test framework
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.arrow)
    testImplementation(libs.kotest.property)
    // testImplementation(libs.kotest.datatest) // Not available
    
    // Process management
    testImplementation(libs.zt.process.executor)
    
    // HTTP client for daemon API testing
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.kotlinx.json)
    
    // gRPC client for daemon testing
    testImplementation(libs.grpc.kotlin.stub)
    testImplementation(libs.grpc.netty.shaded)
    testImplementation(libs.protobuf.java)
    testImplementation(project(":interfaces-rpc-contracts"))
    
    // Platform detection
    testImplementation(libs.os.maven.plugin)
}

tasks {
    test {
        useJUnitPlatform()
        
        // Disable for regular test runs
        enabled = false
    }
    
    // Create a separate task for E2E tests
    register<Test>("e2eTest") {
        description = "Run cross-platform E2E tests with pre-built binaries"
        group = "verification"
        
        useJUnitPlatform()
        
        // Set system properties for binary paths
        systemProperty("scopes.e2e.cli.binary", 
            project.findProperty("e2e.cli.binary") ?: "")
        systemProperty("scopes.e2e.daemon.binary", 
            project.findProperty("e2e.daemon.binary") ?: "")
        systemProperty("scopes.e2e.test.platform", 
            project.findProperty("e2e.test.platform") ?: detectPlatform())
        systemProperty("scopes.e2e.test.arch", 
            project.findProperty("e2e.test.arch") ?: detectArch())
        
        // Extended timeout for daemon operations
        systemProperty("scopes.e2e.daemon.startup.timeout", "30000")
        systemProperty("scopes.e2e.command.timeout", "60000")
        
        // Enable detailed logging
        testLogging {
            events = setOf(
                TestLogEvent.STARTED,
                TestLogEvent.PASSED,
                TestLogEvent.FAILED,
                TestLogEvent.SKIPPED,
                TestLogEvent.STANDARD_OUT,
                TestLogEvent.STANDARD_ERROR
            )
            
            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
            
            // Show output even for passing tests in CI
            if (isCiEnvironment) {
                showStandardStreams = true
            }
        }
        
        // Increase memory for tests
        maxHeapSize = "1024m"
        
        // Run tests sequentially to avoid port conflicts
        maxParallelForks = 1
        
        // Fail fast in CI
        if (isCiEnvironment) {
            failFast = true
        }
    }
}

// Platform detection functions
fun detectPlatform(): String {
    val osName = System.getProperty("os.name").lowercase()
    return when {
        osName.contains("win") -> "win32"
        osName.contains("mac") -> "darwin"
        osName.contains("linux") -> "linux"
        else -> "unknown"
    }
}

fun detectArch(): String {
    val osArch = System.getProperty("os.arch").lowercase()
    return when {
        osArch.contains("amd64") || osArch.contains("x86_64") -> "x64"
        osArch.contains("aarch64") || osArch.contains("arm64") -> "arm64"
        else -> "unknown"
    }
}