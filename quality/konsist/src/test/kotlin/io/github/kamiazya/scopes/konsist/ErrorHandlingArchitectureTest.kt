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

        "production code should not contain 'unknown' string literal fallbacks" {
            // Check all production code (excluding tests)
            Konsist
                .scopeFromProduction()
                .files
                .assertFalse { file ->
                    // Look for patterns like:
                    // - "unknown" as a fallback value
                    // - ?: "unknown"
                    // - else -> "unknown"
                    val hasUnknownFallback = file.text.contains(
                        Regex(
                            """(?i)(?:
                                |\?:\s*"unknown"|
                                |else\s*->\s*"unknown"|
                                |=\s*"unknown"|
                                |return\s+"unknown"
                            )""".trimMargin()
                        )
                    )
                    
                    // Allow "UnknownError" as it's a proper error type name
                    val isProperErrorType = file.text.contains("UnknownError")
                    
                    hasUnknownFallback && !isProperErrorType
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
                            """else\s*->\s*[^{]*(?:
                                |\.copy\(|
                                |Error\(.*?"unknown"|
                                |return\s+\w+Error\(
                            )""".trimMargin()
                        )
                    ) && !file.text.contains("throw")
                }
        }

        "repository implementations should validate data from external sources" {
            Konsist
                .scopeFromDirectory("contexts")
                .classes()
                .filter { it.resideInPackage("..infrastructure..repository..") }
                .filter { it.name.endsWith("Repository") }
                .filter { !it.hasInterfaceModifier }
                .assertFalse { clazz ->
                    // Check if repository has methods that convert from DB without validation
                    // This is a heuristic check - repositories should use .create() methods
                    // or throw exceptions for invalid data
                    val hasRowConversion = clazz.text.contains("row") || clazz.text.contains("Row")
                    val hasValidation = clazz.text.contains(".create(") || 
                                       clazz.text.contains("throw") ||
                                       clazz.text.contains("IllegalStateException") ||
                                       clazz.text.contains("require")
                    
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
                        Regex("""error::class\.simpleName\s*\?:\s*"Unknown"""")
                    )
                    
                    // We want qualified name as primary with simple name as fallback
                    val hasImprovedPattern = file.text.contains(
                        Regex("""error::class\.qualifiedName\s*\?:.*?simpleName""")
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
                        Regex("""return\s+"error"|return\s+"failed"|return\s+"invalid"""")
                    )
                }
        }
    })