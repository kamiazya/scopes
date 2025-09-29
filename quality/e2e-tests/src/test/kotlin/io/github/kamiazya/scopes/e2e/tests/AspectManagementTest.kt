package io.github.kamiazya.scopes.e2e.tests

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.nio.file.Files

/**
 * Simplified E2E tests for aspect management functionality.
 * Uses ProcessBuilder for direct binary testing.
 */
class AspectManagementTest :
    StringSpec({

        val cliBinaryPath = System.getProperty("scopes.e2e.cli.binary")

        "should add and list aspects" {
            val tempDir = Files.createTempDirectory("scopes-e2e-aspect-").toFile()
            val scopesDir = File(tempDir, ".scopes").apply { mkdirs() }

            try {
                val uniqueTitle = "AspectTest-${System.currentTimeMillis()}"

                // Create scope
                val createProcess = ProcessBuilder(cliBinaryPath, "create", uniqueTitle).apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val createExitCode = createProcess.waitFor()
                val createOutput = createProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                createExitCode shouldBe 0
                createOutput shouldContain "Scope created successfully"

                // Extract alias
                val aliasRegex = """Alias: ([a-z]+-[a-z]+-[a-z0-9]+)""".toRegex()
                val alias = aliasRegex.find(createOutput)?.groupValues?.get(1)

                if (alias != null) {
                    // Add aspects
                    val aspectProcess = ProcessBuilder(cliBinaryPath, "aspect", "set", alias, "priority=high").apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val aspectExitCode = aspectProcess.waitFor()
                    val aspectOutput = aspectProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    println("Set aspect output: $aspectOutput")
                    aspectExitCode shouldBe 0
                    aspectOutput shouldContain "Set aspects on scope"

                    // Get scope and verify aspect appears
                    val getProcess = ProcessBuilder(cliBinaryPath, "get", alias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val getExitCode = getProcess.waitFor()
                    val getOutput = getProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    println("Get scope output: $getOutput")
                    getExitCode shouldBe 0
                    getOutput shouldContain uniqueTitle
                    getOutput shouldContain "priority: high"

                    // Clean up
                    val deleteProcess = ProcessBuilder(cliBinaryPath, "delete", alias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val deleteExitCode = deleteProcess.waitFor()
                    if (deleteExitCode == 0) {
                        println("✓ Successfully cleaned up test scope")
                    }
                }
            } finally {
                tempDir.deleteRecursively()
            }
        }

        "should filter scopes by aspects" {
            val tempDir = Files.createTempDirectory("scopes-e2e-aspect-filter-").toFile()
            val scopesDir = File(tempDir, ".scopes").apply { mkdirs() }

            try {
                val uniqueTitle1 = "HighPriorityTest-${System.currentTimeMillis()}"
                val uniqueTitle2 = "LowPriorityTest-${System.currentTimeMillis()}"

                // Create high priority scope
                val createProcess1 = ProcessBuilder(cliBinaryPath, "create", uniqueTitle1).apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                createProcess1.waitFor()
                val createOutput1 = createProcess1.inputStream.readAllBytes().toString(Charsets.UTF_8)
                val aliasRegex = """Alias: ([a-z]+-[a-z]+-[a-z0-9]+)""".toRegex()
                val alias1 = aliasRegex.find(createOutput1)?.groupValues?.get(1)

                // Create low priority scope
                val createProcess2 = ProcessBuilder(cliBinaryPath, "create", uniqueTitle2).apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                createProcess2.waitFor()
                val createOutput2 = createProcess2.inputStream.readAllBytes().toString(Charsets.UTF_8)
                val alias2 = aliasRegex.find(createOutput2)?.groupValues?.get(1)

                if (alias1 != null && alias2 != null) {
                    // Set different priorities
                    ProcessBuilder(cliBinaryPath, "aspect", "set", alias1, "priority=high").apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.start().waitFor()

                    ProcessBuilder(cliBinaryPath, "aspect", "set", alias2, "priority=low").apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.start().waitFor()

                    // Filter by high priority
                    val filterProcess = ProcessBuilder(cliBinaryPath, "list", "-a", "priority=high").apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val filterExitCode = filterProcess.waitFor()
                    val filterOutput = filterProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    println("Filter by priority=high output: $filterOutput")
                    filterExitCode shouldBe 0
                    filterOutput shouldContain uniqueTitle1

                    // Clean up
                    ProcessBuilder(cliBinaryPath, "delete", alias1).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.start().waitFor()

                    ProcessBuilder(cliBinaryPath, "delete", alias2).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.start().waitFor()
                }
            } finally {
                tempDir.deleteRecursively()
            }
        }
    })
