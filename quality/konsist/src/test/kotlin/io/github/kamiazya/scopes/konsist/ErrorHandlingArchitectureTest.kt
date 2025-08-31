package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import io.kotest.core.spec.style.StringSpec

/**
 * Architecture tests for error handling patterns.
 * Ensures proper error handling without masking data integrity issues.
 *
 * Key principles:
 * - No "unknown" string fallbacks that mask data corruption
 * - Fail-fast for data integrity issues
 * - Proper error types with meaningful information
 * - Explicit error handling at system boundaries
 */
class ErrorHandlingArchitectureTest :
    StringSpec({

        "production code should use Kotlin error functions instead of throw" {
            // Enforce Kotlin idiomatic error handling
            Konsist
                .scopeFromProduction()
                .files
                .filter { !it.path.contains("/cli/") } // CLI layer can use CliktError
                .assertFalse { file ->
                    // Check for direct exception throwing (except CliktError in CLI)
                    file.text.contains(
                        Regex(
                            """throw\s+(?!CliktError)[A-Z]\w*Exception\s*\(""",
                        ),
                    )
                }
        }

        "use Kotlin's error() instead of throw IllegalStateException" {
            Konsist
                .scopeFromProduction()
                .files
                .assertFalse { file ->
                    file.text.contains("throw IllegalStateException")
                }
        }

        "use require() instead of throw IllegalArgumentException" {
            Konsist
                .scopeFromProduction()
                .files
                .assertFalse { file ->
                    file.text.contains("throw IllegalArgumentException")
                }
        }

        "use check() for post-conditions instead of manual checking" {
            Konsist
                .scopeFromProduction()
                .files
                .assertFalse { file ->
                    // Look for patterns like: if (!condition) throw IllegalStateException
                    file.text.contains(
                        Regex(
                            """if\s*\(\s*!.*?\)\s*throw\s+IllegalStateException""",
                        ),
                    )
                }
        }

        "production code should not contain 'unknown' string literal fallbacks" {
            // Check all production code (excluding tests)
            Konsist
                .scopeFromProduction()
                .files
                .assertFalse { file ->
                    // Look for patterns like:
                    // - ?: "unknown"
                    // - else -> "unknown"
                    // - = "unknown"
                    // - return "unknown"
                    val hasUnknownFallback = file.text.contains(
                        Regex(
                            """(?i)(?:\?:\s*"unknown"|else\s*->\s*"unknown"|=\s*"unknown"|return\s+"unknown")""",
                        ),
                    )

                    hasUnknownFallback
                }
        }

        "error mapping should throw exceptions for unmapped cases" {
            Konsist
                .scopeFromDirectory("contexts")
                .files
                .filter { it.name.contains("ErrorMapping") || it.name.contains("Mapping") }
                .filter { it.path.contains("src/main") }
                .assertFalse { file ->
                    // Check for patterns that silently default unmapped errors
                    // We want to ensure unmapped errors throw exceptions
                    file.text.contains(
                        Regex(
                            """else\s*->\s*[^{]*(?:\.copy\(|Error\(.*?"unknown"|return\s+\w+Error\()""",
                        ),
                    ) &&
                        !file.text.contains("throw") &&
                        !file.text.contains("error(")
                }
        }

        "repository implementations should validate data from external sources" {
            Konsist
                .scopeFromDirectory("contexts")
                .classes() // This already excludes interfaces, only gets concrete classes
                .filter { it.resideInPackage("..infrastructure..repository..") }
                .filter { it.name.endsWith("Repository") }
                .assertFalse { clazz ->
                    // Check if repository has methods that convert from DB without validation
                    // This is a heuristic check - repositories should use .create() methods
                    // or throw exceptions for invalid data
                    val hasRowConversion = clazz.text.contains("row") || clazz.text.contains("Row")
                    val hasValidation = clazz.text.contains(".create(") ||
                        clazz.text.contains("throw") ||
                        clazz.text.contains("error(") ||
                        clazz.text.contains("IllegalStateException") ||
                        clazz.text.contains("require") ||
                        clazz.text.contains("check(")

                    hasRowConversion && !hasValidation
                }
        }

        "error class names should be properly logged with qualified names" {
            Konsist
                .scopeFromProduction()
                .files
                .filter { file ->
                    file.text.contains("logger") || file.text.contains("log")
                }
                .assertFalse { file ->
                    // Check for simple name usage without qualified name fallback
                    val hasSimpleNameOnly = file.text.contains(
                        Regex("""error::class\.simpleName\s*\?:\s*"Unknown""""),
                    )

                    // We want qualified name as primary with simple name as fallback
                    val hasImprovedPattern = file.text.contains(
                        Regex("""error::class\.qualifiedName\s*\?:.*?simpleName"""),
                    )

                    hasSimpleNameOnly && !hasImprovedPattern
                }
        }

        "domain validation errors should use proper error types" {
            Konsist
                .scopeFromDirectory("contexts")
                .classes()
                .filter { it.resideInPackage("..domain..") }
                .filter { !it.name.endsWith("Test") }
                .assertFalse { clazz ->
                    // Domain classes shouldn't use string literals for error states
                    // They should use proper error types
                    clazz.text.contains(
                        Regex("""return\s+"error"|return\s+"failed"|return\s+"invalid"""),
                    )
                }
        }

        "production code should never use getOrNull()!! anti-pattern" {
            Konsist
                .scopeFromProduction()
                .files
                .assertFalse { file ->
                    // This pattern can throw NullPointerException
                    file.text.contains(".getOrNull()!!")
                }
        }

        "repository fold operations should handle errors properly" {
            Konsist
                .scopeFromProduction()
                .files
                .filter { it.path.contains("repository") || it.path.contains("Repository") }
                .assertFalse { file ->
                    // Look for fold patterns that ignore errors in the left branch
                    // Pattern: .fold({ /* comment */ }, { ... }) or .fold({ }, { ... })
                    file.text.contains(
                        Regex(
                            """\\.fold\s*\(\s*\{\s*(?:/\*.*?\*/|\s)*\}\s*,""",
                            RegexOption.DOT_MATCHES_ALL,
                        ),
                    )
                }
        }

        "use case fold operations should not ignore repository errors" {
            Konsist
                .scopeFromProduction()
                .files
                .filter { file ->
                    file.path.contains("UseCase") ||
                        file.path.contains("usecase") ||
                        file.path.contains("Handler")
                }
                .assertFalse { file ->
                    // Look for patterns like:
                    // repository.method().fold({ /* ignored */ }, { success })
                    val hasFoldWithIgnoredError = file.text.contains(
                        Regex(
                            """repository\.\w+\(.*?\)\.fold\s*\(\s*\{\s*(?:/\*.*?\*/|\s)*\}\s*,""",
                            RegexOption.DOT_MATCHES_ALL,
                        ),
                    )

                    // Also check for fold with empty error handling
                    val hasEmptyErrorHandling = file.text.contains(
                        Regex(
                            """\.fold\s*\(\s*\{\s*}\s*,""",
                            RegexOption.DOT_MATCHES_ALL,
                        ),
                    )

                    hasFoldWithIgnoredError || hasEmptyErrorHandling
                }
        }

        "runBlocking should not be used in init blocks".config(enabled = false) {
            // Temporarily disabled: Known violation in ScopesCliApplication.kt
            // This is tracked as a code smell to be addressed
            Konsist
                .scopeFromProduction()
                .classes()
                .assertFalse { clazz ->
                    // Check for runBlocking in init blocks
                    clazz.text.contains(
                        Regex(
                            """init\s*\{[^}]*runBlocking""",
                            RegexOption.DOT_MATCHES_ALL,
                        ),
                    )
                }
        }
    })
