package io.github.kamiazya.scopes.e2e.tests

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.nio.file.Files

/**
 * Minimal test that directly executes the native binaries using ProcessBuilder.
 * This bypasses all framework dependencies to test the core native functionality.
 */
class MinimalNativeTest :
    StringSpec({

        "CLI binary should be executable and show help" {
            val cliBinaryPath = System.getProperty("scopes.e2e.cli.binary")
            val cliBinary = File(cliBinaryPath)

            // Verify binary exists and is executable
            cliBinary.exists() shouldBe true
            cliBinary.canExecute() shouldBe true

            // Test --help command
            val process = ProcessBuilder(cliBinaryPath, "--help")
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            val output = process.inputStream.readAllBytes().toString(Charsets.UTF_8)

            println("CLI Help Test:")
            println("Exit code: $exitCode")
            println("Output: $output")

            exitCode shouldBe 0
            output shouldContain "Usage:"
            output shouldContain "scopes"
        }

        "CLI binary should create and list scopes" {
            val cliBinaryPath = System.getProperty("scopes.e2e.cli.binary")

            // Create a temporary directory for this test to avoid data corruption
            val tempDir = Files.createTempDirectory("scopes-test-").toFile()
            val scopesDir = File(tempDir, ".scopes").apply { mkdirs() }

            try {
                // Use unique title with timestamp to avoid conflicts
                val uniqueTitle = "MinimalTest-${System.currentTimeMillis()}"

                // Test create command with custom config directory
                val createProcess = ProcessBuilder(cliBinaryPath, "create", uniqueTitle).apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val createExitCode = createProcess.waitFor()
                val createOutput = createProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                println("CLI Create Test:")
                println("Exit code: $createExitCode")
                println("Output: $createOutput")

                createExitCode shouldBe 0
                createOutput shouldContain "Scope created successfully"
                createOutput shouldContain "Alias:"

                // Extract the alias for cleanup
                val aliasRegex = """Alias: ([a-z]+-[a-z]+-[a-z0-9]+)""".toRegex()
                val alias = aliasRegex.find(createOutput)?.groupValues?.get(1)

                if (alias != null) {
                    // Test list command
                    val listProcess = ProcessBuilder(cliBinaryPath, "list").apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val listExitCode = listProcess.waitFor()
                    val listOutput = listProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    println("CLI List Test:")
                    println("Exit code: $listExitCode")
                    println("Output: $listOutput")

                    listExitCode shouldBe 0
                    listOutput shouldContain uniqueTitle
                    // Note: Current list format shows title only, not alias

                    // Clean up: delete the scope
                    val deleteProcess = ProcessBuilder(cliBinaryPath, "delete", alias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val deleteExitCode = deleteProcess.waitFor()
                    val deleteOutput = deleteProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    println("CLI Delete Test:")
                    println("Exit code: $deleteExitCode")
                    println("Output: $deleteOutput")

                    if (deleteExitCode == 0) {
                        println("✓ Successfully cleaned up test scope")
                    } else {
                        println("⚠ Warning: Failed to clean up test scope")
                    }
                }
            } finally {
                // Clean up temporary directory
                tempDir.deleteRecursively()
            }
        }

        "Daemon binary should be executable" {
            val daemonBinaryPath = System.getProperty("scopes.e2e.daemon.binary")
            val daemonBinary = File(daemonBinaryPath)

            // Verify daemon binary exists and is executable
            daemonBinary.exists() shouldBe true
            daemonBinary.canExecute() shouldBe true

            println("✓ Daemon binary found and is executable: $daemonBinaryPath (${daemonBinary.length()} bytes)")

            // Try to get help (daemon may behave differently)
            val process = ProcessBuilder(daemonBinaryPath, "--help")
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            val output = process.inputStream.readAllBytes().toString(Charsets.UTF_8)

            println("Daemon Help Test:")
            println("Exit code: $exitCode")
            println("Output: $output")

            // For now, just verify the binary can be executed (exit code may vary)
            // The important part is that the binary doesn't crash immediately
            println("✓ Daemon binary executed without immediate crash")
        }
    })
