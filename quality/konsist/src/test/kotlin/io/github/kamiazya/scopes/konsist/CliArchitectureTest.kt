package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Architecture tests for CLI patterns.
 * Ensures proper CLI implementation patterns based on AI review feedback.
 *
 * Key principles:
 * - Use cross-platform shell completion patterns
 * - Apply consistent filtering across all command branches
 * - Use parallel processing for performance-critical operations
 * - Avoid shell-specific constructs in completion candidates
 */
class CliArchitectureTest :
    StringSpec({

        "CLI commands should use cross-platform completion candidates without shell redirections" {
            Konsist
                .scopeFromDirectory("interfaces/cli")
                .files
                .filter { it.name.contains("Command") }
                .assertFalse { file ->
                    // Check for shell-specific redirections in completion candidates
                    file.text.contains(
                        Regex("""CompletionCandidates\.Custom\.fromStdout\s*\(\s*"[^"]*2>/dev/null[^"]*""""),
                    ) ||
                        file.text.contains(
                            Regex("""CompletionCandidates\.Custom\.fromStdout\s*\(\s*"[^"]*\|\|[^"]*""""),
                        )
                }
        }

        "CLI commands with filtering should apply filters consistently across all branches" {
            Konsist
                .scopeFromDirectory("interfaces/cli")
                .files
                .filter { it.name.contains("Command") }
                .filter { it.text.contains("aspectFilters") }
                .assertTrue { file ->
                    val whenBlocks = file.text.split("when").drop(1) // Get all when block contents

                    // Check that if any branch applies filtering, all branches should
                    val branchesWithFiltering = whenBlocks.count { block ->
                        block.contains("filterByAspects") || block.contains("filteredScopes")
                    }

                    val totalBranches = whenBlocks.count { block ->
                        block.contains("scopes ->") // Pattern for scope processing
                    }

                    // If filtering exists, it should be in all branches
                    branchesWithFiltering == 0 || branchesWithFiltering == totalBranches
                }
        }

        "completion commands should use parallel processing for performance" {
            Konsist
                .scopeFromDirectory("interfaces/cli")
                .files
                .filter { it.name.contains("CompletionCommand") }
                .filter { it.text.contains("listChildren") }
                .assertTrue { file ->
                    // Should use coroutines for parallel processing
                    file.text.contains("coroutineScope") &&
                        (file.text.contains("async") || file.text.contains("launch")) &&
                        (file.text.contains("awaitAll") || file.text.contains("joinAll"))
                }
        }

        "CLI command option parsing should validate and trim user input" {
            Konsist
                .scopeFromDirectory("interfaces/cli")
                .files
                .filter { it.name.contains("Command") }
                .filter { it.text.contains(".split(") } // Commands that parse user input
                .assertTrue { file ->
                    // Should trim parsed parts and validate non-empty
                    file.text.contains(".trim()") &&
                        (file.text.contains("isEmpty()") || file.text.contains("isBlank()"))
                }
        }

        "hidden CLI commands should be properly marked as hidden" {
            Konsist
                .scopeFromDirectory("interfaces/cli")
                .classes()
                .filter { it.name.contains("Command") }
                .filter { it.text.contains("name = \"_") } // Commands starting with underscore
                .assertTrue { clazz ->
                    // Should have hiddenFromHelp = true property (Clikt 5.x)
                    clazz.text.contains("hiddenFromHelp = true")
                }
        }

        "CLI error output should go to stderr not stdout" {
            Konsist
                .scopeFromDirectory("interfaces/cli")
                .files
                .filter { it.name.contains("Command") }
                .assertTrue { file ->
                    // Check that error/warning messages are properly routed to stderr
                    // This regex matches echo statements with Error/Warning that DON'T have stderr routing
                    val errorWithoutStderr = Regex(
                        """echo\s*\(\s*"(?:Error|Warning)[^"]*"\s*\)""",
                    )

                    // This regex matches echo statements that DO have stderr routing
                    val errorWithStderr = Regex(
                        """echo\s*\(\s*"(?:Error|Warning)[^"]*"\s*(?:,\s*err(?:or)?\s*=\s*true|,\s*stderr\b|>\&2)\s*\)""",
                    )

                    // Either no error messages at all, or if there are, they must be routed to stderr
                    !file.text.contains(errorWithoutStderr) ||
                        file.text.contains(errorWithStderr)
                }
        }
    })
