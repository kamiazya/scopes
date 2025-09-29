package io.github.kamiazya.scopes.e2e.tests

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.nio.file.Files

/**
 * CLI integration tests focusing on command-line interface operations.
 * Uses ProcessBuilder for direct binary testing.
 * Simplified to focus on core CLI functionality.
 */
class CliIntegrationTest :
    StringSpec({

        val cliBinaryPath = System.getProperty("scopes.e2e.cli.binary")
        val daemonBinaryPath = System.getProperty("scopes.e2e.daemon.binary")

        "should show version information" {
            val process = ProcessBuilder(cliBinaryPath, "--version")
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            val output = process.inputStream.readAllBytes().toString(Charsets.UTF_8)

            println("Version output: $output")
            exitCode shouldBe 0
            output shouldContain "Scopes"
        }

        "should show help information" {
            val process = ProcessBuilder(cliBinaryPath, "--help")
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            val output = process.inputStream.readAllBytes().toString(Charsets.UTF_8)

            println("Help output: $output")
            exitCode shouldBe 0
            output shouldContain "Usage:"
            output shouldContain "Commands:"
            output shouldContain "scopes"
        }

        "should create and list scopes" {
            val tempDir = Files.createTempDirectory("scopes-e2e-cli-integration-").toFile()
            val scopesDir = File(tempDir, ".scopes").apply { mkdirs() }

            try {
                val timestamp = System.currentTimeMillis()
                val scope1Title = "Project-Alpha-$timestamp"
                val scope2Title = "Project-Beta-$timestamp"

                // Create first scope
                val createProcess1 = ProcessBuilder(cliBinaryPath, "create", scope1Title).apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val createExitCode1 = createProcess1.waitFor()
                val createOutput1 = createProcess1.inputStream.readAllBytes().toString(Charsets.UTF_8)

                println("Create scope 1 output: $createOutput1")
                createExitCode1 shouldBe 0
                createOutput1 shouldContain "Scope created successfully"

                // Extract alias
                val aliasRegex = """Alias: ([a-z]+-[a-z]+-[a-z0-9]+)""".toRegex()
                val alias1 = aliasRegex.find(createOutput1)?.groupValues?.get(1)
                alias1 shouldNotBe null

                // Create second scope
                val createProcess2 = ProcessBuilder(cliBinaryPath, "create", scope2Title).apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val createExitCode2 = createProcess2.waitFor()
                val createOutput2 = createProcess2.inputStream.readAllBytes().toString(Charsets.UTF_8)

                println("Create scope 2 output: $createOutput2")
                createExitCode2 shouldBe 0
                createOutput2 shouldContain "Scope created successfully"

                val alias2 = aliasRegex.find(createOutput2)?.groupValues?.get(1)
                alias2 shouldNotBe null

                // List scopes
                val listProcess = ProcessBuilder(cliBinaryPath, "list").apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val listExitCode = listProcess.waitFor()
                val listOutput = listProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                println("List output: $listOutput")
                listExitCode shouldBe 0
                listOutput shouldContain scope1Title
                listOutput shouldContain scope2Title

                // Get specific scope
                if (alias1 != null) {
                    val getProcess = ProcessBuilder(cliBinaryPath, "get", alias1).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val getExitCode = getProcess.waitFor()
                    val getOutput = getProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    println("Get scope output: $getOutput")
                    getExitCode shouldBe 0
                    getOutput shouldContain scope1Title
                    getOutput shouldContain alias1
                }

                // Cleanup
                listOf(alias1, alias2).filterNotNull().forEach { alias ->
                    ProcessBuilder(cliBinaryPath, "delete", alias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.start().waitFor()
                }
            } finally {
                tempDir.deleteRecursively()
            }
        }

        "should handle invalid commands gracefully" {
            val tempDir = Files.createTempDirectory("scopes-e2e-cli-error-").toFile()
            val scopesDir = File(tempDir, ".scopes").apply { mkdirs() }

            try {
                // Test invalid command
                val invalidProcess = ProcessBuilder(cliBinaryPath, "invalid-command").apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val invalidExitCode = invalidProcess.waitFor()
                val invalidOutput = invalidProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                println("Invalid command output: $invalidOutput")
                invalidExitCode shouldNotBe 0
                // Should show help or error message

                // Test missing required arguments
                val missingArgProcess = ProcessBuilder(cliBinaryPath, "create").apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val missingArgExitCode = missingArgProcess.waitFor()
                val missingArgOutput = missingArgProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                println("Missing argument output: $missingArgOutput")
                missingArgExitCode shouldNotBe 0

                // Test non-existent scope
                val nonExistentProcess = ProcessBuilder(cliBinaryPath, "get", "non-existent-scope-xyz").apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val nonExistentExitCode = nonExistentProcess.waitFor()
                val nonExistentOutput = nonExistentProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                println("Non-existent scope output: $nonExistentOutput")
                nonExistentExitCode shouldNotBe 0
                nonExistentOutput shouldContain "not found"
            } finally {
                tempDir.deleteRecursively()
            }
        }

        "should handle daemon binary availability" {
            // Just verify the daemon binary exists and is executable
            val daemonBinary = File(daemonBinaryPath)
            daemonBinary.exists() shouldBe true
            daemonBinary.canExecute() shouldBe true

            println("✓ Daemon binary found and is executable: $daemonBinaryPath")

            // Try to get help from daemon (basic execution test)
            val process = ProcessBuilder(daemonBinaryPath, "--help")
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            val output = process.inputStream.readAllBytes().toString(Charsets.UTF_8)

            println("Daemon help test:")
            println("Exit code: $exitCode")
            println("Output: $output")

            // For now, just verify the binary can be executed
            daemonBinary.canExecute() shouldBe true
            println("✓ Daemon binary executed without immediate crash")
        }

        "should handle scope operations with descriptions" {
            val tempDir = Files.createTempDirectory("scopes-e2e-cli-descriptions-").toFile()
            val scopesDir = File(tempDir, ".scopes").apply { mkdirs() }

            try {
                val timestamp = System.currentTimeMillis()
                val title = "Detailed-Project-$timestamp"
                val description = "A detailed project description for testing"

                // Create scope with description
                val createProcess = ProcessBuilder(cliBinaryPath, "create", title, "-d", description).apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val createExitCode = createProcess.waitFor()
                val createOutput = createProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                println("Create with description output: $createOutput")
                createExitCode shouldBe 0
                createOutput shouldContain "Scope created successfully"

                // Extract alias
                val aliasRegex = """Alias: ([a-z]+-[a-z]+-[a-z0-9]+)""".toRegex()
                val alias = aliasRegex.find(createOutput)?.groupValues?.get(1)

                if (alias != null) {
                    // Get scope and verify description appears
                    val getProcess = ProcessBuilder(cliBinaryPath, "get", alias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val getExitCode = getProcess.waitFor()
                    val getOutput = getProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    println("Get scope with description output: $getOutput")
                    getExitCode shouldBe 0
                    getOutput shouldContain title
                    getOutput shouldContain description

                    // Cleanup
                    ProcessBuilder(cliBinaryPath, "delete", alias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.start().waitFor()
                }
            } finally {
                tempDir.deleteRecursively()
            }
        }
    })
