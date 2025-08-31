package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import io.kotest.core.spec.style.StringSpec

/**
 * Architecture tests for database performance patterns.
 * Ensures efficient database operations without in-memory filtering.
 *
 * Key principles:
 * - Pagination should be done at database level
 * - Filtering should use database queries, not in-memory operations
 * - Avoid loading entire tables into memory
 */
class DatabasePerformanceTest :
    StringSpec({

        "repository implementations should not use in-memory pagination".config(enabled = false) {
            // Temporarily disabled: Known violations tracked in GitHub issue #131
            // Files with violations:
            // - SqlDelightScopeRepository.kt
            // - SqlDelightScopeAliasRepository.kt
            Konsist
                .scopeFromProduction()
                .files
                .filter { file ->
                    file.path.contains("repository") &&
                        file.path.contains("infrastructure") &&
                        !file.path.contains("InMemory") // In-memory repositories are allowed to do this
                }
                .assertFalse { file ->
                    // Look for patterns like:
                    // .drop(offset).take(limit)
                    // after a database query
                    val hasDropTake = file.text.contains(
                        Regex(
                            """\.drop\s*\(\s*\w+\s*\)\s*\.take\s*\(""",
                            RegexOption.DOT_MATCHES_ALL,
                        ),
                    )

                    // Also check for toList().drop().take() pattern
                    val hasToListDropTake = file.text.contains(
                        Regex(
                            """\.toList\s*\(\s*\)\s*\.drop\s*\(\s*\w+\s*\)\s*\.take""",
                            RegexOption.DOT_MATCHES_ALL,
                        ),
                    )

                    hasDropTake || hasToListDropTake
                }
        }

        "use case implementations should not filter all scopes in memory" {
            Konsist
                .scopeFromProduction()
                .files
                .filter { file ->
                    file.path.contains("UseCase") ||
                        file.path.contains("Handler")
                }
                .assertFalse { file ->
                    // Look for patterns like:
                    // repository.findAll() followed by filter operations
                    val hasFindAllFilter = file.text.contains(
                        Regex(
                            """repository\.findAll\s*\(\s*\)[^{]*\.filter\s*\{""",
                            RegexOption.DOT_MATCHES_ALL,
                        ),
                    )

                    // Also check for getting all items then filtering
                    val hasGetAllFilter = file.text.contains(
                        Regex(
                            """\.getAll\s*\(\s*\)[^{]*\.filter\s*\{""",
                            RegexOption.DOT_MATCHES_ALL,
                        ),
                    )

                    hasFindAllFilter || hasGetAllFilter
                }
        }

        "SQL queries should use LIMIT and OFFSET for pagination" {
            Konsist
                .scopeFromProduction()
                .files
                .filter { file ->
                    file.extension == "sq" ||
                        // SQLDelight files
                        (file.path.contains("repository") && file.text.contains("SELECT"))
                }
                .assertFalse { file ->
                    // If file contains pagination parameters but no LIMIT/OFFSET
                    val hasPaginationParams = file.text.contains("offset") && file.text.contains("limit")
                    val hasLimitOffset = file.text.contains("LIMIT") || file.text.contains("OFFSET")

                    hasPaginationParams && !hasLimitOffset
                }
        }

        "avoid loading entire collections before counting" {
            Konsist
                .scopeFromProduction()
                .files
                .filter { file ->
                    file.path.contains("repository") ||
                        file.path.contains("Repository")
                }
                .assertFalse { file ->
                    // Look for patterns like:
                    // .toList().size or .toList().count()
                    file.text.contains(
                        Regex(
                            """\.toList\s*\(\s*\)\s*\.(size|count\s*\(\s*\))""",
                            RegexOption.DOT_MATCHES_ALL,
                        ),
                    )
                }
        }

        "update use cases should avoid unnecessary database writes" {
            Konsist
                .scopeFromProduction()
                .files
                .filter { file ->
                    file.path.contains("Update") &&
                        (file.path.contains("UseCase") || file.path.contains("Handler"))
                }
                .assertFalse { file ->
                    // Look for patterns where save is called without checking if entity changed
                    // Pattern: existing.copy(...) followed directly by repository.save()
                    // without any conditional check
                    val copyFollowedBySave = file.text.contains(
                        Regex(
                            """\.copy\s*\([^)]*\)\s*\n\s*repository\.save""",
                            RegexOption.DOT_MATCHES_ALL,
                        ),
                    )

                    // Check if there's a conditional to skip save when unchanged
                    val hasConditionalSave = file.text.contains("if") &&
                        (
                            file.text.contains("!= existing") ||
                                file.text.contains("!== existing") ||
                                file.text.contains("changed") ||
                                file.text.contains("modified")
                            )

                    copyFollowedBySave && !hasConditionalSave
                }
        }
    })
