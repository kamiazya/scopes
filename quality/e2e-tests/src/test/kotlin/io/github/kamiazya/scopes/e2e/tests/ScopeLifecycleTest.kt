package io.github.kamiazya.scopes.e2e.tests

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.nio.file.Files

/**
 * Comprehensive E2E tests for scope lifecycle management.
 * Uses ProcessBuilder for direct binary testing.
 */
class ScopeLifecycleTest :
    StringSpec({

        val cliBinaryPath = System.getProperty("scopes.e2e.cli.binary")

        "should create, update, and delete a scope successfully" {
            val tempDir = Files.createTempDirectory("scopes-e2e-lifecycle-").toFile()
            val scopesDir = File(tempDir, ".scopes").apply { mkdirs() }

            try {
                val uniqueTitle = "LifecycleTest-${System.currentTimeMillis()}"
                val uniqueDescription = "Testing full lifecycle ${System.currentTimeMillis()}"

                // Create scope
                val createProcess = ProcessBuilder(cliBinaryPath, "create", uniqueTitle, "-d", uniqueDescription).apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val createExitCode = createProcess.waitFor()
                val createOutput = createProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                println("Create output: $createOutput")
                createExitCode shouldBe 0
                createOutput shouldContain "Scope created successfully"

                // Extract alias
                val aliasRegex = """Alias: ([a-z]+-[a-z]+-[a-z0-9]+)""".toRegex()
                val alias = aliasRegex.find(createOutput)?.groupValues?.get(1)
                alias shouldNotBe null

                if (alias != null) {
                    // Get and verify initial state
                    val getProcess1 = ProcessBuilder(cliBinaryPath, "get", alias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val getExitCode1 = getProcess1.waitFor()
                    val getOutput1 = getProcess1.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    println("Get initial output: $getOutput1")
                    getExitCode1 shouldBe 0
                    getOutput1 shouldContain uniqueTitle
                    getOutput1 shouldContain uniqueDescription

                    // Update title
                    val updatedTitle = "Updated-$uniqueTitle"
                    val updateProcess1 = ProcessBuilder(cliBinaryPath, "update", alias, "--title", updatedTitle).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val updateExitCode1 = updateProcess1.waitFor()
                    val updateOutput1 = updateProcess1.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    println("Update title output: $updateOutput1")
                    updateExitCode1 shouldBe 0
                    updateOutput1 shouldContain "Updated scope"

                    // Update description
                    val updatedDescription = "Updated description for lifecycle test"
                    val updateProcess2 = ProcessBuilder(cliBinaryPath, "update", alias, "--description", updatedDescription).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val updateExitCode2 = updateProcess2.waitFor()
                    updateExitCode2 shouldBe 0

                    // Verify updates
                    val getProcess2 = ProcessBuilder(cliBinaryPath, "get", alias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val getExitCode2 = getProcess2.waitFor()
                    val getOutput2 = getProcess2.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    println("Get updated output: $getOutput2")
                    getExitCode2 shouldBe 0
                    getOutput2 shouldContain updatedTitle
                    getOutput2 shouldContain updatedDescription

                    // Delete
                    val deleteProcess = ProcessBuilder(cliBinaryPath, "delete", alias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val deleteExitCode = deleteProcess.waitFor()
                    val deleteOutput = deleteProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    println("Delete output: $deleteOutput")
                    deleteExitCode shouldBe 0
                    deleteOutput shouldContain "Successfully deleted scope"

                    // Verify deletion
                    val getProcess3 = ProcessBuilder(cliBinaryPath, "get", alias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val getExitCode3 = getProcess3.waitFor()
                    getExitCode3 shouldNotBe 0 // Should fail since scope is deleted
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
                val uniqueTitle = "CustomAliasScope-${System.currentTimeMillis()}"

                val createProcess = ProcessBuilder(cliBinaryPath, "create", uniqueTitle, "--alias", customAlias).apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val createExitCode = createProcess.waitFor()
                val createOutput = createProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                println("Create with custom alias output: $createOutput")
                createExitCode shouldBe 0
                createOutput shouldContain "Scope created successfully"
                createOutput shouldContain customAlias

                // Verify we can access by custom alias
                val getProcess = ProcessBuilder(cliBinaryPath, "get", customAlias).apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val getExitCode = getProcess.waitFor()
                val getOutput = getProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                println("Get by custom alias output: $getOutput")
                getExitCode shouldBe 0
                getOutput shouldContain uniqueTitle

                // Cleanup
                val deleteProcess = ProcessBuilder(cliBinaryPath, "delete", customAlias).apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                deleteProcess.waitFor()
            } finally {
                tempDir.deleteRecursively()
            }
        }

        "should handle hierarchical scopes" {
            val tempDir = Files.createTempDirectory("scopes-e2e-hierarchy-").toFile()
            val scopesDir = File(tempDir, ".scopes").apply { mkdirs() }

            try {
                val timestamp = System.currentTimeMillis()
                val parentTitle = "ParentScope-$timestamp"
                val child1Title = "Child1-$timestamp"
                val child2Title = "Child2-$timestamp"
                val grandchildTitle = "Grandchild-$timestamp"

                // Create parent
                val parentProcess = ProcessBuilder(cliBinaryPath, "create", parentTitle, "-d", "The parent").apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val parentExitCode = parentProcess.waitFor()
                val parentOutput = parentProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                println("Create parent output: $parentOutput")
                parentExitCode shouldBe 0
                parentOutput shouldContain "Scope created successfully"

                val aliasRegex = """Alias: ([a-z]+-[a-z]+-[a-z0-9]+)""".toRegex()
                val parentAlias = aliasRegex.find(parentOutput)?.groupValues?.get(1)
                parentAlias shouldNotBe null

                if (parentAlias != null) {
                    // Create child 1
                    val child1Process = ProcessBuilder(cliBinaryPath, "create", child1Title, "--parent", parentAlias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val child1ExitCode = child1Process.waitFor()
                    val child1Output = child1Process.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    println("Create child1 output: $child1Output")
                    child1ExitCode shouldBe 0
                    child1Output shouldContain "Scope created successfully"

                    val child1Alias = aliasRegex.find(child1Output)?.groupValues?.get(1)
                    child1Alias shouldNotBe null

                    // Create child 2
                    val child2Process = ProcessBuilder(cliBinaryPath, "create", child2Title, "--parent", parentAlias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val child2ExitCode = child2Process.waitFor()
                    val child2Output = child2Process.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    println("Create child2 output: $child2Output")
                    child2ExitCode shouldBe 0
                    child2Output shouldContain "Scope created successfully"

                    val child2Alias = aliasRegex.find(child2Output)?.groupValues?.get(1)

                    if (child1Alias != null) {
                        // Create grandchild
                        val grandchildProcess = ProcessBuilder(cliBinaryPath, "create", grandchildTitle, "--parent", child1Alias).apply {
                            environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                        }.redirectErrorStream(true).start()

                        val grandchildExitCode = grandchildProcess.waitFor()
                        val grandchildOutput = grandchildProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                        println("Create grandchild output: $grandchildOutput")
                        grandchildExitCode shouldBe 0
                        grandchildOutput shouldContain "Scope created successfully"

                        val grandchildAlias = aliasRegex.find(grandchildOutput)?.groupValues?.get(1)

                        // List and verify hierarchy
                        val listProcess = ProcessBuilder(cliBinaryPath, "list").apply {
                            environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                        }.redirectErrorStream(true).start()

                        val listExitCode = listProcess.waitFor()
                        val listOutput = listProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                        println("List output: $listOutput")
                        listExitCode shouldBe 0
                        listOutput shouldContain parentTitle
                        listOutput shouldContain child1Title
                        listOutput shouldContain child2Title
                        listOutput shouldContain grandchildTitle

                        // Get parent and verify it shows correctly
                        val parentGetProcess = ProcessBuilder(cliBinaryPath, "get", parentAlias).apply {
                            environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                        }.redirectErrorStream(true).start()

                        val parentGetExitCode = parentGetProcess.waitFor()
                        val parentGetOutput = parentGetProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                        println("Get parent output: $parentGetOutput")
                        parentGetExitCode shouldBe 0
                        parentGetOutput shouldContain parentTitle
                    }

                    // Cleanup - delete parent (should handle children cascade)
                    val deleteProcess = ProcessBuilder(cliBinaryPath, "delete", parentAlias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    deleteProcess.waitFor()
                }
            } finally {
                tempDir.deleteRecursively()
            }
        }

        "should handle batch operations efficiently" {
            val tempDir = Files.createTempDirectory("scopes-e2e-batch-").toFile()
            val scopesDir = File(tempDir, ".scopes").apply { mkdirs() }

            try {
                val scopeAliases = mutableListOf<String>()
                val timestamp = System.currentTimeMillis()

                // Create multiple scopes (reduce from 10 to 5 for faster test)
                repeat(5) { index ->
                    val title = "BatchScope-$index-$timestamp"
                    val description = "Description $index for batch test"

                    val createProcess = ProcessBuilder(cliBinaryPath, "create", title, "-d", description).apply {
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
                        scopeAliases.add(alias)
                    }
                }

                // List all - should show all created scopes
                val listProcess = ProcessBuilder(cliBinaryPath, "list").apply {
                    environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                }.redirectErrorStream(true).start()

                val listExitCode = listProcess.waitFor()
                val listOutput = listProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                println("List all scopes output: $listOutput")
                listExitCode shouldBe 0

                // Verify all scopes appear in list (by checking titles)
                repeat(5) { index ->
                    listOutput shouldContain "BatchScope-$index-$timestamp"
                }

                // Update all scopes
                scopeAliases.forEachIndexed { index, alias ->
                    val updatedTitle = "UpdatedBatchScope-$index-$timestamp"
                    val updateProcess = ProcessBuilder(cliBinaryPath, "update", alias, "--title", updatedTitle).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val updateExitCode = updateProcess.waitFor()
                    updateExitCode shouldBe 0
                }

                // Verify updates
                scopeAliases.forEachIndexed { index, alias ->
                    val getProcess = ProcessBuilder(cliBinaryPath, "get", alias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val getExitCode = getProcess.waitFor()
                    val getOutput = getProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    getExitCode shouldBe 0
                    getOutput shouldContain "UpdatedBatchScope-$index-$timestamp"
                }

                // Cleanup
                scopeAliases.forEach { alias ->
                    val deleteProcess = ProcessBuilder(cliBinaryPath, "delete", alias).apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    deleteProcess.waitFor()
                }
            } finally {
                tempDir.deleteRecursively()
            }
        }
    })
