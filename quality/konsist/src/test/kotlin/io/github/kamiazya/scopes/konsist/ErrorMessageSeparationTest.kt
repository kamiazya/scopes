// This file is temporarily disabled due to memory issues
// TODO: Re-enable after fixing memory issues and test structure

package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Tests to ensure proper separation of error information and messages.
 * Validates that domain errors contain structured data, not pre-formatted messages.
 */
class ErrorMessageSeparationTest :
    StringSpec({

        "domain errors should not contain message properties or formatting logic" {
            Konsist
                .scopeFromDirectory("contexts")
                .files
                .filter { it.path.contains("/domain/error/") }
                .flatMap { it.classes() }
                .filter { it.name.endsWith("Error") }
                .assertFalse { clazz ->
                    // Check for properties that suggest pre-formatted messages
                    clazz.properties().any { prop ->
                        prop.name in listOf("message", "errorMessage", "displayMessage", "userMessage")
                    }
                }
        }

        "contract errors should not contain pre-formatted messages" {
            Konsist
                .scopeFromDirectory("contracts")
                .files
                .filter { it.path.contains("/errors/") }
                .flatMap { it.classes() + it.interfaces() }
                .filter { it.name.endsWith("Error") }
                .assertFalse { errorType ->
                    // Contract errors shouldn't have message properties
                    errorType.properties().any { prop ->
                        prop.name in listOf("message", "errorMessage", "displayMessage")
                    }
                }
        }

        "domain errors should contain rich types not primitive strings" {
            Konsist
                .scopeFromDirectory("contexts")
                .files
                .filter { it.path.contains("/domain/error/") }
                .flatMap { it.classes() }
                .filter { it.name.endsWith("Error") && !it.hasSealedModifier }
                .filter { !it.hasAbstractModifier }
                .assertTrue { clazz ->
                    val props = clazz.properties()
                    // Should have at least one property
                    props.isNotEmpty() &&
                        // All properties should be typed (not just String)
                        props.all { prop ->
                            val type = prop.type?.name ?: ""
                            // Allow specific rich types
                            type in listOf(
                                "ScopeId",
                                "AspectKey",
                                "ContextViewId",
                                "ValidationFailure",
                                "ErrorType",
                                "Int",
                                "Long",
                            ) ||
                                type.endsWith("Type") ||
                                type.endsWith("Id") ||
                                type.endsWith("Key") ||
                                // Allow nullable and collections of rich types
                                type.contains("?") ||
                                type.contains("List<") ||
                                type.contains("Set<")
                        }
                }
        }

        "application errors should use simple types" {
            Konsist
                .scopeFromDirectory("contexts")
                .files
                .filter { it.path.contains("/application/error/") }
                .flatMap { it.classes() }
                .filter { it.name.endsWith("Error") }
                .assertTrue { clazz ->
                    // Application layer can use simpler error types
                    clazz.properties().all { prop ->
                        val type = prop.type?.name ?: ""
                        // Application errors can use String for IDs and messages
                        type in listOf("String", "Int", "Long", "Boolean") ||
                            type.contains("List<") ||
                            type.contains("?")
                    }
                }
        }

        "CLI commands should use message mappers for error formatting" {
            Konsist
                .scopeFromDirectory("interfaces/cli")
                .files
                .filter { it.path.contains("/commands/") }
                .flatMap { it.classes() }
                .filter { clazz ->
                    // Look for command classes
                    clazz.name.endsWith("Command") &&
                        // That handle errors
                        clazz.text.contains("fold") &&
                        clazz.text.contains("ifLeft")
                }
                .assertTrue { clazz ->
                    // Should use ErrorMessageMapper for formatting
                    clazz.text.contains("ErrorMessageMapper") ||
                        clazz.text.contains("ContractErrorMessageMapper")
                }
        }
    })
