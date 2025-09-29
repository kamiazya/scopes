package io.github.kamiazya.scopes.e2e.tests

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.nio.file.Files

/**
 * Simplified E2E tests for alias management functionality.
 * Uses ProcessBuilder for direct binary testing.
 */
class AliasManagementTest :
    StringSpec({

        val cliBinaryPath = System.getProperty("scopes.e2e.cli.binary")

        "should create scope with auto-generated alias and add custom alias" {
            val tempDir = Files.createTempDirectory("scopes-e2e-alias-").toFile()
            val scopesDir = File(tempDir, ".scopes").apply { mkdirs() }

            try {
                val uniqueTitle = "AliasTest-${System.currentTimeMillis()}"

                // Create scope
                val createProcess = ProcessBuilder(cliBinaryPath, "create", uniqueTitle).apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val createExitCode = createProcess.waitFor()
                val createOutput = createProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                createExitCode shouldBe 0
                createOutput shouldContain "Scope created successfully"

                // Extract canonical alias
                val aliasRegex = """Alias: ([a-z]+-[a-z]+-[a-z0-9]+)""".toRegex()
                val canonicalAlias = aliasRegex.find(createOutput)?.groupValues?.get(1)

                if (canonicalAlias != null) {
                    // Add custom alias
                    val customAlias = "test-alias-${System.currentTimeMillis()}"
                    val addAliasProcess = ProcessBuilder(cliBinaryPath, "alias", "add", canonicalAlias, customAlias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val addExitCode = addAliasProcess.waitFor()
                    val addOutput = addAliasProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    println("Add alias output: $addOutput")
                    addExitCode shouldBe 0
                    addOutput shouldContain "assigned to scope"

                    // List aliases
                    val listAliasProcess = ProcessBuilder(cliBinaryPath, "alias", "list", canonicalAlias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val listExitCode = listAliasProcess.waitFor()
                    val listOutput = listAliasProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    println("List aliases output: $listOutput")
                    listExitCode shouldBe 0
                    listOutput shouldContain canonicalAlias
                    listOutput shouldContain customAlias

                    // Test resolving custom alias
                    val getProcess = ProcessBuilder(cliBinaryPath, "get", customAlias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val getExitCode = getProcess.waitFor()
                    val getOutput = getProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    println("Get via custom alias output: $getOutput")
                    getExitCode shouldBe 0
                    getOutput shouldContain uniqueTitle

                    // Clean up
                    val deleteProcess = ProcessBuilder(cliBinaryPath, "delete", canonicalAlias).apply {
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

        "should create scope with custom alias" {
            val tempDir = Files.createTempDirectory("scopes-e2e-custom-alias-").toFile()
            val scopesDir = File(tempDir, ".scopes").apply { mkdirs() }

            try {
                val customAlias = "my-custom-alias-${System.currentTimeMillis()}"
                val uniqueTitle = "CustomAliasTest-${System.currentTimeMillis()}"

                // Create scope with custom alias
                val createProcess = ProcessBuilder(cliBinaryPath, "create", uniqueTitle, "--alias", customAlias).apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val createExitCode = createProcess.waitFor()
                val createOutput = createProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                println("Create with custom alias output: $createOutput")
                createExitCode shouldBe 0
                createOutput shouldContain "Scope created successfully"
                createOutput shouldContain customAlias

                // Verify we can get the scope using the custom alias
                val getProcess = ProcessBuilder(cliBinaryPath, "get", customAlias).apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val getExitCode = getProcess.waitFor()
                val getOutput = getProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                getExitCode shouldBe 0
                getOutput shouldContain uniqueTitle
                getOutput shouldContain customAlias

                // Clean up
                val deleteProcess = ProcessBuilder(cliBinaryPath, "delete", customAlias).apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val deleteExitCode = deleteProcess.waitFor()
                if (deleteExitCode == 0) {
                    println("✓ Successfully cleaned up test scope")
                }
            } finally {
                tempDir.deleteRecursively()
            }
        }
    })
