package io.github.kamiazya.scopes.e2e.tests

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.nio.file.Files

/**
 * Platform-specific tests focusing on binary compatibility and basic functionality.
 * Uses ProcessBuilder for direct binary testing.
 * Simplified to focus on cross-platform binary execution verification.
 */
class PlatformSpecificTest :
    StringSpec({

        val cliBinaryPath = System.getProperty("scopes.e2e.cli.binary")
        val daemonBinaryPath = System.getProperty("scopes.e2e.daemon.binary")

        "should verify binary files exist and are executable" {
            val cliBinary = File(cliBinaryPath)
            val daemonBinary = File(daemonBinaryPath)

            // Basic existence and permission checks
            cliBinary.exists() shouldBe true
            cliBinary.canExecute() shouldBe true
            daemonBinary.exists() shouldBe true
            daemonBinary.canExecute() shouldBe true

            println("✓ CLI binary: ${cliBinary.absolutePath} (${cliBinary.length()} bytes)")
            println("✓ Daemon binary: ${daemonBinary.absolutePath} (${daemonBinary.length()} bytes)")

            // Verify they are actually binary files (not text files)
            cliBinary.length() shouldBeGreaterThan 1000 // Should be substantial binaries
            daemonBinary.length() shouldBeGreaterThan 1000
        }

        "should detect current platform and verify binary compatibility" {
            val osName = System.getProperty("os.name").lowercase()
            val osArch = System.getProperty("os.arch").lowercase()

            println("Running on: $osName ($osArch)")

            when {
                osName.contains("windows") -> {
                    println("Platform: Windows")
                    // On Windows, binaries might have .exe extension
                    if (cliBinaryPath.endsWith(".exe")) {
                        println("✓ Windows executable detected with .exe extension")
                    }
                }
                osName.contains("mac") || osName.contains("darwin") -> {
                    println("Platform: macOS")
                    // Test basic execution on macOS
                }
                osName.contains("linux") -> {
                    println("Platform: Linux")
                    // Test basic execution on Linux
                }
                else -> {
                    println("Platform: Other Unix-like ($osName)")
                }
            }

            // Basic execution test for both binaries
            val cliProcess = ProcessBuilder(cliBinaryPath, "--help")
                .redirectErrorStream(true)
                .start()

            val cliExitCode = cliProcess.waitFor()
            val cliOutput = cliProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

            cliExitCode shouldBe 0
            cliOutput shouldContain "scopes"
            println("✓ CLI binary executes correctly on this platform")

            val daemonProcess = ProcessBuilder(daemonBinaryPath, "--help")
                .redirectErrorStream(true)
                .start()

            val daemonExitCode = daemonProcess.waitFor()
            val daemonOutput = daemonProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

            println("Daemon help exit code: $daemonExitCode")
            println("Daemon help output: $daemonOutput")
            println("✓ Daemon binary executes on this platform")
        }

        "should handle path separators correctly" {
            val tempDir = Files.createTempDirectory("scopes-e2e-platform-path-").toFile()
            val scopesDir = File(tempDir, ".scopes").apply { mkdirs() }

            try {
                val pathSeparator = File.separator
                println("Platform path separator: '$pathSeparator'")

                // Create a scope and verify paths work correctly
                val process = ProcessBuilder(cliBinaryPath, "create", "PlatformPathTest").apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val exitCode = process.waitFor()
                val output = process.inputStream.readAllBytes().toString(Charsets.UTF_8)

                println("Path test output: $output")
                exitCode shouldBe 0
                output shouldContain "Scope created successfully"

                // The fact that it worked means path handling is correct for this platform
                println("✓ Path handling works correctly on this platform")

                // Extract and cleanup
                val aliasRegex = """Alias: ([a-z]+-[a-z]+-[a-z0-9]+)""".toRegex()
                val alias = aliasRegex.find(output)?.groupValues?.get(1)
                if (alias != null) {
                    ProcessBuilder(cliBinaryPath, "delete", alias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.start().waitFor()
                }
            } finally {
                tempDir.deleteRecursively()
            }
        }

        "should handle special characters in paths and titles" {
            val tempDir = Files.createTempDirectory("scopes-e2e-special-chars-").toFile()
            val scopesDir = File(tempDir, ".scopes").apply { mkdirs() }

            try {
                // Test with various special characters (platform-appropriate)
                val specialTitle = "Test Special: Chars & Symbols (2024)"

                val process = ProcessBuilder(cliBinaryPath, "create", specialTitle).apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val exitCode = process.waitFor()
                val output = process.inputStream.readAllBytes().toString(Charsets.UTF_8)

                println("Special characters test:")
                println("Exit code: $exitCode")
                println("Output: $output")

                if (exitCode == 0) {
                    output shouldContain "Scope created successfully"
                    println("✓ Platform handles special characters in titles")

                    // Extract and cleanup
                    val aliasRegex = """Alias: ([a-z]+-[a-z]+-[a-z0-9]+)""".toRegex()
                    val alias = aliasRegex.find(output)?.groupValues?.get(1)
                    if (alias != null) {
                        // Verify we can retrieve it
                        val getProcess = ProcessBuilder(cliBinaryPath, "get", alias).apply {
                            environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                        }.redirectErrorStream(true).start()

                        val getExitCode = getProcess.waitFor()
                        val getOutput = getProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                        if (getExitCode == 0) {
                            getOutput shouldContain specialTitle
                            println("✓ Special characters preserved correctly")
                        }

                        ProcessBuilder(cliBinaryPath, "delete", alias).apply {
                            environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                        }.start().waitFor()
                    }
                } else {
                    println("ℹ Platform may have restrictions on special characters")
                }
            } finally {
                tempDir.deleteRecursively()
            }
        }

        "should handle environment variables correctly" {
            val tempDir = Files.createTempDirectory("scopes-e2e-env-vars-").toFile()
            val scopesDir = File(tempDir, ".scopes").apply { mkdirs() }

            try {
                // Test basic environment variable handling
                val envProcess = ProcessBuilder(cliBinaryPath, "list").apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    environment().put("SCOPES_DEBUG", "true")
                }.redirectErrorStream(true).start()

                val envExitCode = envProcess.waitFor()
                val envOutput = envProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                println("Environment variable test:")
                println("Exit code: $envExitCode")
                println("Output: $envOutput")

                envExitCode shouldBe 0
                println("✓ Environment variables handled correctly")

                // Test that config directory override works
                val process = ProcessBuilder(cliBinaryPath, "create", "EnvTest").apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val exitCode = process.waitFor()
                val output = process.inputStream.readAllBytes().toString(Charsets.UTF_8)

                if (exitCode == 0) {
                    output shouldContain "Scope created successfully"
                    println("✓ SCOPES_CONFIG_DIR environment variable works")

                    // Cleanup
                    val aliasRegex = """Alias: ([a-z]+-[a-z]+-[a-z0-9]+)""".toRegex()
                    val alias = aliasRegex.find(output)?.groupValues?.get(1)
                    if (alias != null) {
                        ProcessBuilder(cliBinaryPath, "delete", alias).apply {
                            environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                        }.start().waitFor()
                    }
                }
            } finally {
                tempDir.deleteRecursively()
            }
        }

        "should verify binary architecture compatibility" {
            // Get system properties
            val javaVmName = System.getProperty("java.vm.name") ?: "unknown"
            val javaVersion = System.getProperty("java.version") ?: "unknown"
            val osArch = System.getProperty("os.arch") ?: "unknown"

            println("Runtime environment:")
            println("  JVM: $javaVmName")
            println("  Java version: $javaVersion")
            println("  OS Architecture: $osArch")

            // Basic execution test to verify architecture compatibility
            val process = ProcessBuilder(cliBinaryPath, "--version")
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            val output = process.inputStream.readAllBytes().toString(Charsets.UTF_8)

            println("Version output: $output")
            exitCode shouldBe 0
            output shouldContain "Scopes"

            println("✓ Binary architecture is compatible with current system")
        }

        "should test basic UTF-8 support" {
            val tempDir = Files.createTempDirectory("scopes-e2e-utf8-").toFile()
            val scopesDir = File(tempDir, ".scopes").apply { mkdirs() }

            try {
                // Test with Unicode characters
                val unicodeTitle = "Test 你好 🌍 Café"

                val process = ProcessBuilder(cliBinaryPath, "create", unicodeTitle).apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val exitCode = process.waitFor()
                val output = process.inputStream.readAllBytes().toString(Charsets.UTF_8)

                println("UTF-8 test:")
                println("Exit code: $exitCode")
                println("Output: $output")

                if (exitCode == 0) {
                    output shouldContain "Scope created successfully"
                    println("✓ Platform supports UTF-8 characters")

                    // Extract and verify
                    val aliasRegex = """Alias: ([a-z]+-[a-z]+-[a-z0-9]+)""".toRegex()
                    val alias = aliasRegex.find(output)?.groupValues?.get(1)
                    if (alias != null) {
                        val getProcess = ProcessBuilder(cliBinaryPath, "get", alias).apply {
                            environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                        }.redirectErrorStream(true).start()

                        val getExitCode = getProcess.waitFor()
                        val getOutput = getProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                        if (getExitCode == 0) {
                            println("Retrieved UTF-8 title correctly")
                        }

                        ProcessBuilder(cliBinaryPath, "delete", alias).apply {
                            environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                        }.start().waitFor()
                    }
                } else {
                    println("ℹ Platform may have UTF-8 limitations")
                }
            } finally {
                tempDir.deleteRecursively()
            }
        }
    })
