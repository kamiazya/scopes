package io.github.kamiazya.scopes.e2e.tests

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import java.nio.file.Files
import kotlin.system.measureTimeMillis

/**
 * Advanced E2E tests for query functionality and performance scenarios.
 * Uses ProcessBuilder for direct binary testing.
 * Simplified to focus on currently implemented features.
 */
class AdvancedQueryTest :
    StringSpec({

        val cliBinaryPath = System.getProperty("scopes.e2e.cli.binary")

        // Helper function to create scope and return alias
        fun createTestScope(title: String, scopesDir: File): String? {
            val createProcess = ProcessBuilder(cliBinaryPath, "create", title).apply {
                environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
            }.redirectErrorStream(true).start()

            val createExitCode = createProcess.waitFor()
            val createOutput = createProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

            return if (createExitCode == 0) {
                val aliasRegex = """Alias: ([a-z]+-[a-z]+-[a-z0-9]+)""".toRegex()
                aliasRegex.find(createOutput)?.groupValues?.get(1)
            } else {
                null
            }
        }

        "should handle basic aspect filtering" {
            val tempDir = Files.createTempDirectory("scopes-e2e-advanced-query-").toFile()
            val scopesDir = File(tempDir, ".scopes").apply { mkdirs() }

            try {
                val timestamp = System.currentTimeMillis()
                val highPriorityTitle = "HighPriorityTask-$timestamp"
                val lowPriorityTitle = "LowPriorityTask-$timestamp"
                val mediumPriorityTitle = "MediumPriorityTask-$timestamp"

                // Create scopes with different priorities
                val highAlias = createTestScope(highPriorityTitle, scopesDir)
                val lowAlias = createTestScope(lowPriorityTitle, scopesDir)
                val mediumAlias = createTestScope(mediumPriorityTitle, scopesDir)

                if (highAlias != null && lowAlias != null && mediumAlias != null) {
                    // Set different priority aspects
                    ProcessBuilder(cliBinaryPath, "aspect", "set", highAlias, "priority=high").apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.start().waitFor()

                    ProcessBuilder(cliBinaryPath, "aspect", "set", lowAlias, "priority=low").apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.start().waitFor()

                    ProcessBuilder(cliBinaryPath, "aspect", "set", mediumAlias, "priority=medium").apply {
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
                    filterOutput shouldContain highPriorityTitle
                    filterOutput shouldNotContain lowPriorityTitle
                    filterOutput shouldNotContain mediumPriorityTitle

                    // Cleanup
                    listOf(highAlias, lowAlias, mediumAlias).forEach { alias ->
                        ProcessBuilder(cliBinaryPath, "delete", alias).apply {
                            environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                        }.start().waitFor()
                    }
                }
            } finally {
                tempDir.deleteRecursively()
            }
        }

        "should handle multiple aspect filtering" {
            val tempDir = Files.createTempDirectory("scopes-e2e-multi-aspect-").toFile()
            val scopesDir = File(tempDir, ".scopes").apply { mkdirs() }

            try {
                val timestamp = System.currentTimeMillis()
                val bugTitle = "BugTask-$timestamp"
                val featureTitle = "FeatureTask-$timestamp"
                val taskTitle = "RegularTask-$timestamp"

                // Create scopes with different types and priorities
                val bugAlias = createTestScope(bugTitle, scopesDir)
                val featureAlias = createTestScope(featureTitle, scopesDir)
                val taskAlias = createTestScope(taskTitle, scopesDir)

                if (bugAlias != null && featureAlias != null && taskAlias != null) {
                    // Set type and priority aspects
                    ProcessBuilder(cliBinaryPath, "aspect", "set", bugAlias, "type=bug", "priority=high").apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.start().waitFor()

                    ProcessBuilder(cliBinaryPath, "aspect", "set", featureAlias, "type=feature", "priority=high").apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.start().waitFor()

                    ProcessBuilder(cliBinaryPath, "aspect", "set", taskAlias, "type=task", "priority=low").apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.start().waitFor()

                    // Filter by type=bug
                    val typeFilterProcess = ProcessBuilder(cliBinaryPath, "list", "-a", "type=bug").apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val typeFilterExitCode = typeFilterProcess.waitFor()
                    val typeFilterOutput = typeFilterProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    println("Filter by type=bug output: $typeFilterOutput")
                    typeFilterExitCode shouldBe 0
                    typeFilterOutput shouldContain bugTitle
                    typeFilterOutput shouldNotContain featureTitle
                    typeFilterOutput shouldNotContain taskTitle

                    // Filter by priority=high
                    val priorityFilterProcess = ProcessBuilder(cliBinaryPath, "list", "-a", "priority=high").apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val priorityFilterExitCode = priorityFilterProcess.waitFor()
                    val priorityFilterOutput = priorityFilterProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    println("Filter by priority=high output: $priorityFilterOutput")
                    priorityFilterExitCode shouldBe 0
                    priorityFilterOutput shouldContain bugTitle
                    priorityFilterOutput shouldContain featureTitle
                    priorityFilterOutput shouldNotContain taskTitle

                    // Cleanup
                    listOf(bugAlias, featureAlias, taskAlias).forEach { alias ->
                        ProcessBuilder(cliBinaryPath, "delete", alias).apply {
                            environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                        }.start().waitFor()
                    }
                }
            } finally {
                tempDir.deleteRecursively()
            }
        }

        "should handle performance with multiple scopes" {
            val tempDir = Files.createTempDirectory("scopes-e2e-performance-").toFile()
            val scopesDir = File(tempDir, ".scopes").apply { mkdirs() }

            try {
                val scopeAliases = mutableListOf<String>()
                val timestamp = System.currentTimeMillis()
                val totalScopes = 20 // Reduced for faster testing

                println("Creating $totalScopes scopes for performance test...")

                // Create multiple scopes with varied aspects
                val createTime = measureTimeMillis {
                    repeat(totalScopes) { index ->
                        val title = "PerfTestScope-$index-$timestamp"
                        val alias = createTestScope(title, scopesDir)

                        if (alias != null) {
                            scopeAliases.add(alias)

                            // Set varied aspects
                            val priority = when (index % 3) {
                                0 -> "high"
                                1 -> "medium"
                                else -> "low"
                            }
                            val status = when (index % 2) {
                                0 -> "active"
                                else -> "completed"
                            }

                            ProcessBuilder(cliBinaryPath, "aspect", "set", alias, "priority=$priority", "status=$status").apply {
                                environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                            }.start().waitFor()
                        }
                    }
                }

                println("Created $totalScopes scopes in ${createTime}ms")

                // Test query performance
                val queryTime = measureTimeMillis {
                    // List all scopes
                    val listProcess = ProcessBuilder(cliBinaryPath, "list").apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val listExitCode = listProcess.waitFor()
                    val listOutput = listProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    listExitCode shouldBe 0
                    val allCount = listOutput.lines().count { it.contains("PerfTestScope") }
                    allCount shouldBe totalScopes

                    // Filter by priority
                    val filterProcess = ProcessBuilder(cliBinaryPath, "list", "-a", "priority=high").apply {
                        environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                    }.redirectErrorStream(true).start()

                    val filterExitCode = filterProcess.waitFor()
                    val filterOutput = filterProcess.inputStream.readAllBytes().toString(Charsets.UTF_8)

                    filterExitCode shouldBe 0
                    val filteredCount = filterOutput.lines().count { it.contains("PerfTestScope") }
                    filteredCount shouldBeGreaterThan 0
                }

                println("Query operations completed in ${queryTime}ms")

                // Cleanup
                println("Cleaning up $totalScopes scopes...")
                val deleteTime = measureTimeMillis {
                    scopeAliases.forEach { alias ->
                        ProcessBuilder(cliBinaryPath, "delete", alias).apply {
                            environment().put("SCOPES_CONFIG_DIR", scopesDir.absolutePath)
                        }.start().waitFor()
                    }
                }
                println("Deleted $totalScopes scopes in ${deleteTime}ms")
            } finally {
                tempDir.deleteRecursively()
            }
        }
    })
