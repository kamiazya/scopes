package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Architecture tests for Contract layer (Ports and Adapters pattern).
 * Ensures proper structure and naming conventions for contract interfaces.
 *
 * Key principles:
 * - Port interfaces define the contract between bounded contexts
 * - Port names should clearly indicate their bounded context
 * - Contracts should use immutable data structures
 * - Errors should be comprehensive and well-structured
 */
class ContractLayerArchitectureTest :
    StringSpec({

        "Port interfaces should follow naming convention" {
            Konsist
                .scopeFromDirectory("contracts")
                .interfaces()
                .filter { it.name.endsWith("Port") }
                .assertTrue { port ->
                    // Port names should be descriptive and end with 'Port'
                    // Examples: UserPreferencesPort, ScopeManagementPort
                    val validName = port.name.matches(Regex("^[A-Z][a-zA-Z]+Port$"))
                    val hasProperPackage = port.resideInPackage("..contracts..")
                    validName && hasProperPackage
                }
        }

        "Port interfaces should have clear method names" {
            Konsist
                .scopeFromDirectory("contracts")
                .interfaces()
                .filter { it.name.endsWith("Port") }
                .assertTrue { port ->
                    // Allow ContextViewPort as special case
                    if (port.name == "ContextViewPort") return@assertTrue true

                    port.functions().all { function ->
                        // Method names should be verbs that clearly indicate the action
                        // Examples: getPreference, createScope, updateScope
                        val name = function.name
                        val isValidVerb = name.matches(
                            Regex("^(get|create|update|delete|find|search|list|check|validate|execute|add|remove|set|rename|register|clear)[A-Z].*"),
                        ) ||
                            name == "clearActiveContext" // Special case
                        isValidVerb
                    }
                }
        }

        "Port interfaces should use Either for error handling" {
            Konsist
                .scopeFromDirectory("contracts")
                .interfaces()
                .filter { it.name.endsWith("Port") }
                .assertTrue { port ->
                    // Allow ports that use contract response pattern
                    if (port.name == "ContextViewPort" ||
                        port.name == "ContextViewQueryPort" ||
                        port.name == "AspectQueryPort"
                    ) {
                        return@assertTrue true
                    }

                    port.functions().all { function ->
                        // All port methods should return Either for explicit error handling
                        function.returnType?.name?.contains("Either") == true
                    }
                }
        }

        "Contract errors should be sealed interfaces" {
            Konsist
                .scopeFromDirectory("contracts")
                .interfaces()
                .filter { it.resideInPackage("..errors..") }
                .filter { it.name.endsWith("Error") }
                .assertTrue { error ->
                    // Contract errors should be sealed for exhaustive when expressions
                    error.hasSealedModifier
                }
        }

        "Command classes should be immutable" {
            Konsist
                .scopeFromDirectory("contracts")
                .classes()
                .filter { it.resideInPackage("..commands..") }
                .filter { it.name.endsWith("Command") }
                .assertTrue { command ->
                    // Commands should be data classes for immutability
                    command.hasDataModifier
                }
        }

        "Query classes should be immutable" {
            Konsist
                .scopeFromDirectory("contracts")
                .classes()
                .filter { it.resideInPackage("..queries..") }
                .filter { it.name.endsWith("Query") }
                .assertTrue { query ->
                    // Queries should be data classes for immutability
                    query.hasDataModifier
                }
        }

        "Result classes should be immutable" {
            Konsist
                .scopeFromDirectory("contracts")
                .classes()
                .filter { it.resideInPackage("..results..") }
                .filter { it.name.endsWith("Result") }
                .assertTrue { result ->
                    // Results should be data classes or sealed classes
                    result.hasDataModifier || result.hasSealedModifier
                }
        }

        "Contract layer should not depend on implementation details" {
            Konsist
                .scopeFromDirectory("contracts")
                .files
                .filter { it.path.contains("src/main") }
                .assertFalse { file ->
                    file.imports.any { import ->
                        // Contracts should not import from implementation layers
                        import.name.contains(".domain.") ||
                            import.name.contains(".application.") ||
                            import.name.contains(".infrastructure.") ||
                            import.name.contains(".interfaces.")
                    }
                }
        }

        "Port interfaces should have comprehensive documentation" {
            Konsist
                .scopeFromDirectory("contracts")
                .interfaces()
                .filter { it.name.endsWith("Port") }
                .assertTrue { port ->
                    // All port interfaces should have KDoc comments
                    port.hasKDoc
                }
        }

        "Port interface methods should have documentation" {
            Konsist
                .scopeFromDirectory("contracts")
                .interfaces()
                .filter { it.name.endsWith("Port") }
                .assertTrue { port ->
                    // All public methods should have KDoc comments
                    port.functions()
                        .filter { it.hasPublicModifier }
                        .all { function ->
                            function.hasKDoc
                        }
                }
        }
    })
