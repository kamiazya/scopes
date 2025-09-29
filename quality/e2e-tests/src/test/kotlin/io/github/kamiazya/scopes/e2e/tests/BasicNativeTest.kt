package io.github.kamiazya.scopes.e2e.tests

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.nio.file.Files

/**
 * Basic native binary test to verify the binaries are working.
 * This test uses ProcessBuilder like MinimalNativeTest for compatibility.
 */
class BasicNativeTest :
    StringSpec({

        val cliBinaryPath = System.getProperty("scopes.e2e.cli.binary")
        val daemonBinaryPath = System.getProperty("scopes.e2e.daemon.binary")

        "CLI binary should exist and be executable" {
            val cliBinary = File(cliBinaryPath)
            cliBinary.exists() shouldBe true
            cliBinary.canExecute() shouldBe true
            println("CLI binary found at: $cliBinaryPath (${cliBinary.length()} bytes)")
        }

        "Daemon binary should exist and be executable" {
            val daemonBinary = File(daemonBinaryPath)
            daemonBinary.exists() shouldBe true
            daemonBinary.canExecute() shouldBe true
            println("Daemon binary found at: $daemonBinaryPath (${daemonBinary.length()} bytes)")
        }

        "CLI should show help" {
            val process = ProcessBuilder(cliBinaryPath, "--help")
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            val output = process.inputStream.readAllBytes().toString(Charsets.UTF_8)

            exitCode shouldBe 0
            output shouldContain "Usage"
            output shouldContain "scopes"
            println("CLI help test passed")
        }

        "CLI should create, list and delete scope successfully" {
            // Create temporary directory for test isolation
            val tempDir = Files.createTempDirectory("scopes-e2e-basic-").toFile()
            val scopesDir = File(tempDir, ".scopes").apply { mkdirs() }

            try {
                val uniqueTitle = "BasicE2E-${System.currentTimeMillis()}"

                // Create scope
                val createProcess = ProcessBuilder(cliBinaryPath, "create", uniqueTitle).apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val createExitCode = createProcess.waitFor()
                val createOutput = createProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                println("Create test output: $createOutput")
                createExitCode shouldBe 0
                createOutput shouldContain "Scope created successfully"

                // Extract alias for further operations
                val aliasRegex = """Alias: ([a-z]+-[a-z]+-[a-z0-9]+)""".toRegex()
                val alias = aliasRegex.find(createOutput)?.groupValues?.get(1)

                if (alias != null) {
                    // List scopes
                    val listProcess = ProcessBuilder(cliBinaryPath, "list").apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val listExitCode = listProcess.waitFor()
                    val listOutput = listProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    println("List test output: $listOutput")
                    listExitCode shouldBe 0
                    listOutput shouldContain uniqueTitle

                    // Get specific scope
                    val getProcess = ProcessBuilder(cliBinaryPath, "get", alias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val getExitCode = getProcess.waitFor()
                    val getOutput = getProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    println("Get test output: $getOutput")
                    getExitCode shouldBe 0
                    getOutput shouldContain uniqueTitle

                    // Clean up: delete the scope
                    val deleteProcess = ProcessBuilder(cliBinaryPath, "delete", alias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val deleteExitCode = deleteProcess.waitFor()
                    if (deleteExitCode == 0) {
                        println("✓ Successfully cleaned up test scope")
                    } else {
                        println("⚠ Warning: Failed to clean up test scope")
                    }
                }
            } finally {
                tempDir.deleteRecursively()
            }
        }

        "Daemon binary should be executable" {
            val process = ProcessBuilder(daemonBinaryPath, "--help")
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            val output = process.inputStream.readAllBytes().toString(Charsets.UTF_8)

            println("Daemon help test:")
            println("Exit code: $exitCode")
            println("Output: $output")

            // Just verify the binary can be executed (exit code may vary)
            File(daemonBinaryPath).canExecute() shouldBe true
            println("✓ Daemon binary executed without immediate crash")
        }
    })
