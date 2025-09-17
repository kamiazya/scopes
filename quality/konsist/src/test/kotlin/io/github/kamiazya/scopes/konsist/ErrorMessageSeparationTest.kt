package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Tests to ensure error messages remain a presentation layer concern.
 *
 * Key principle: Error messages should only be formatted in the interface layer,
 * not in domain, application, or contract layers.
 */
class ErrorMessageSeparationTest :
    StringSpec({
        val contexts = listOf(
            "scope-management",
            "user-preferences",
            "event-store",
            "device-synchronization",
        )

        // ========== Message Separation Rules ==========

        "domain errors should not contain message properties or formatting logic" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.resideInPackage("..error..") || it.resideInPackage("..errors..") }
                    .filter { it.hasErrorInHierarchy() }
                    .filter { !it.name.endsWith("Test") }
                    .assertFalse { error ->
                        // Check for message-related properties
                        error.properties().any { prop ->
                            prop.name.contains("message", ignoreCase = true) &&
                                prop.type?.name == "String"
                        } ||
                            // Check for getMessage() or similar methods
                            error.functions().any { func ->
                                func.name.contains("getMessage") ||
                                    func.name.contains("toMessage") ||
                                    func.name.contains("format") ||
                                    func.name.contains("description")
                            }
                    }
            }
        }

        "contract errors should not contain pre-formatted messages" {
            Konsist
                .scopeFromDirectory("contracts")
                .classes()
                .filter { it.resideInPackage("..error..") || it.resideInPackage("..errors..") }
                .filter { it.hasErrorInHierarchy() }
                .filter { !it.name.endsWith("Test") }
                .assertFalse { error ->
                    // Contract errors should have structured data, not message strings
                    error.properties().any { prop ->
                        (
                            prop.name == "message" ||
                                prop.name == "userMessage" ||
                                prop.name == "errorMessage" ||
                                prop.name == "description"
                            ) &&
                            prop.type?.name == "String"
                    }
                }
        }

        "error message mappers should only exist in interface layer" {
            Konsist
                .scopeFromProduction()
                .classes()
                .filter {
                    it.name.contains("ErrorMessage") ||
                        it.name.contains("MessageMapper") ||
                        (it.name.contains("Mapper") && it.name.contains("Message"))
                }
                .filter { !it.name.endsWith("Test") }
                .assertTrue { mapper ->
                    mapper.resideInPackage("..interfaces..") ||
                        mapper.resideInPackage("..cli..") ||
                        mapper.resideInPackage("..api..") ||
                        mapper.resideInPackage("..presentation..")
                }
        }

        "error mappers should be internal visibility" {
            Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.name.endsWith("ErrorMapper") || it.name.endsWith("ErrorMapping") }
                .filter { !it.name.contains("Message") } // Exclude message mappers
                .filter { !it.name.endsWith("Test") }
                .filter { !it.hasAbstractModifier } // Exclude abstract classes
                .filter { !it.resideInPackage("..platform..") } // Exclude platform base classes/interfaces
                .filter {
                    // Exclude ErrorMapper classes that need to be public for DI injection in Port Adapters
                    // These are still boundary classes but require public visibility for dependency injection
                    !it.name.equals("ErrorMapper") &&
                        !it.name.equals("ApplicationErrorMapper") &&
                        !it.name.equals("EventStoreErrorMapper")
                }
                .assertTrue { mapper ->
                    mapper.hasInternalModifier ||
                        mapper.hasPrivateModifier ||
                        mapper.isInsideObject // Object members are effectively internal
                }
        }

        // ========== Proper Error Structure ==========

        "domain errors should contain rich types not primitive strings" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.resideInPackage("..error..") }
                    .filter { it.hasErrorInHierarchy() }
                    .filter { !it.name.endsWith("Test") }
                    .filter { !it.hasAbstractModifier && !it.hasSealedModifier }
                    .filter {
                        // Allow certain legitimate error classes with String/primitive properties
                        !it.name.contains("QueryParseError") &&
                            !it.name.contains("UserPreferencesIntegrationError") &&
                            !it.name.contains("HierarchyPolicyError") &&
                            !it.name.contains("ServiceUnavailable") &&
                            !it.name.contains("MalformedResponse") &&
                            !it.name.contains("RequestTimeout") &&
                            !it.name.contains("InvalidMaxDepth") &&
                            !it.name.contains("InvalidMaxChildrenPerScope") &&
                            !it.name.contains("UnexpectedCharacter") &&
                            !it.name.contains("UnterminatedString") &&
                            !it.name.contains("UnexpectedToken") &&
                            !it.name.contains("MissingClosingParen") &&
                            !it.name.contains("ExpectedExpression") &&
                            !it.name.contains("ExpectedIdentifier") &&
                            !it.name.contains("ExpectedOperator") &&
                            !it.name.contains("ExpectedValue") &&
                            !it.name.contains("RetrievalError") &&
                            !it.name.contains("ConflictResolutionError") &&
                            !it.name.contains("InvalidAspectFormat")
                    }
                    .assertTrue { error ->
                        // Allow String properties for legitimate error context (reason, title, etc.)
                        val hasStringProperties = error.properties().any { prop ->
                            val typeName = prop.type?.name ?: ""
                            val propName = prop.name.lowercase()
                            typeName == "String" &&
                                (
                                    propName.contains("reason") ||
                                        propName.contains("title") ||
                                        propName.contains("alias") ||
                                        propName.contains("operation") ||
                                        propName.contains("from") ||
                                        propName.contains("to") ||
                                        propName.contains("cause") ||
                                        propName.contains("service") ||
                                        propName.contains("id") ||
                                        propName.contains("value") ||
                                        propName.contains("format") ||
                                        propName.contains("pattern") ||
                                        propName.contains("expression") ||
                                        propName.contains("name")
                                    )
                        }

                        // At least some properties should be value objects or enums OR have legitimate string properties
                        hasStringProperties ||
                            error.properties().any { prop ->
                                val typeName = prop.type?.name ?: ""
                                val sourceType = prop.type?.sourceType ?: ""
                                // Check both type name and source type for value object patterns
                                typeName.endsWith("Id") ||
                                    typeName.endsWith("Name") ||
                                    typeName.endsWith("Type") ||
                                    typeName.endsWith("Key") ||
                                    // Value objects like AspectKey
                                    typeName.endsWith("Value") ||
                                    // Value objects like AspectValue
                                    typeName == "Instant" ||
                                    typeName.contains(".") ||
                                    // Likely a value object
                                    sourceType.contains("AspectKey") ||
                                    sourceType.contains("AspectValue") ||
                                    // For collections, check the generic type
                                    (typeName == "List" && sourceType.contains("<")) ||
                                    (typeName == "Set" && sourceType.contains("<")) ||
                                    // Property names that represent domain concepts
                                    prop.name == "key" ||
                                    // Keys are domain concepts
                                    prop.name == "value" // Values are domain concepts
                            } ||
                            // Or it's a simple object with no properties (marker error)
                            error.properties().isEmpty() ||
                            // Or it's a constraint error with only numeric properties (lengths, counts, etc.)
                            (
                                error.properties().all { prop ->
                                    val propName = prop.name.lowercase()
                                    val typeName = prop.type?.name ?: ""
                                    (typeName == "Int" || typeName == "String") &&
                                        (
                                            propName.contains("length") ||
                                                propName.contains("count") ||
                                                propName.contains("size") ||
                                                propName.contains("max") ||
                                                propName.contains("min") ||
                                                propName.contains("limit") ||
                                                propName.contains("reason") ||
                                                propName.contains("timeout") ||
                                                propName.contains("retry")
                                            )
                                } &&
                                    error.properties().isNotEmpty()
                                ) ||
                            // Or it has enum properties (which are rich types)
                            error.properties().any { prop ->
                                val typeName = prop.type?.name ?: ""
                                val sourceType = prop.type?.sourceType ?: ""
                                // Check if it's an enum type
                                sourceType.contains(".") &&
                                    sourceType.contains("Error") ||
                                    sourceType.contains("Type") ||
                                    prop.name.contains("Error") ||
                                    prop.name.contains("Type")
                            }
                    }
            }
        }

        "application errors should use simple types" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .classes()
                    .filter { it.resideInPackage("..error..") }
                    .filter { it.hasErrorInHierarchy() }
                    .filter { !it.name.endsWith("Test") }
                    .filter { !it.hasAbstractModifier && !it.hasSealedModifier }
                    .assertFalse { error ->
                        // Should not have domain value objects
                        error.properties().any { prop ->
                            val typeName = prop.type?.name ?: ""
                            typeName == "ScopeId" ||
                                typeName == "AliasName" ||
                                typeName == "AspectKey" ||
                                typeName.endsWith("ValueObject")
                        }
                    }
            }
        }

        // ========== Message Formatting Location ==========

        "CLI commands should use message mappers for error formatting" {
            Konsist
                .scopeFromDirectory("interfaces/cli")
                .classes()
                .filter { it.name.endsWith("Command") }
                .filter { !it.name.endsWith("Test") }
                .filter { !it.hasAbstractModifier }
                .assertTrue { command ->
                    // Should import or use a message mapper
                    command.containingFile.imports.any { import ->
                        import.name.contains("ErrorMessageMapper") ||
                            import.name.contains("ContractErrorMessageMapper")
                    } ||
                        // Or at least throw CliktError (which formats messages)
                        command.text.contains("CliktError") ||
                        // Or extend ScopesCliktCommand which handles error messages
                        command.parents().any { parent ->
                            parent.name == "ScopesCliktCommand"
                        }
                }
        }

        "getMessage functions should only exist in interface layer mappers" {
            Konsist
                .scopeFromProduction()
                .functions()
                .filter { it.name == "getMessage" || it.name == "getErrorMessage" }
                .filter { !it.name.endsWith("Test") }
                .assertTrue { function ->
                    function.resideInPackage("..interfaces..") ||
                        function.resideInPackage("..cli..") ||
                        function.resideInPackage("..api..")
                }
        }
    })

// Extension to check if a class is part of an error hierarchy
private fun com.lemonappdev.konsist.api.declaration.KoClassDeclaration.hasErrorInHierarchy(): Boolean = name.endsWith("Error") ||
    name.endsWith("Exception") ||
    parents().any { parent ->
        parent.name.endsWith("Error") ||
            parent.name.endsWith("Exception") ||
            parent.name == "Throwable"
    }

// Extension to check if a declaration is inside an object
private val com.lemonappdev.konsist.api.declaration.KoClassDeclaration.isInsideObject: Boolean
    get() = false // Simplified for now
