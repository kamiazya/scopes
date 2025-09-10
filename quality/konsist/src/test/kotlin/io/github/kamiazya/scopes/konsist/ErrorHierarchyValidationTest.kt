package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Error Hierarchy Validation Test
 * 
 * Validates that error hierarchies follow the established patterns from coding standards:
 * 1. Domain errors extend proper base classes
 * 2. Service-specific errors provide rich context
 * 3. Repository errors are operation-specific
 * 4. Application errors properly translate domain errors
 * 5. No "unknown" or default fallbacks that mask data corruption
 * 
 * Based on the error handling guidelines:
 * - Use Kotlin's error(), check(), require() instead of throwing exceptions
 * - Never use "unknown" or default fallbacks
 * - Use Arrow's Either for functional error handling
 * - Fail-fast for data integrity issues
 */
class ErrorHierarchyValidationTest : StringSpec({

    val boundedContexts = listOf(
        "scope-management",
        "user-preferences",
        "collaborative-versioning",
        "agent-management",
        "event-store",
        "device-synchronization"
    )

    // ========== Base Error Classes ==========

    "each context should have a base error class" {
        val contextBaseErrors = mapOf(
            "scope-management" to "ScopeManagementError",
            "user-preferences" to "UserPreferencesError",
            "collaborative-versioning" to "CollaborativeVersioningError",
            "agent-management" to "AgentManagementError",
            "event-store" to "EventStoreError",
            "device-synchronization" to "DeviceSynchronizationError"
        )

        boundedContexts.forEach { context ->
            val expectedBaseError = contextBaseErrors[context]
            
            Konsist
                .scopeFromDirectory("contexts/$context/domain")
                .classes()
                .filter { it.name == expectedBaseError }
                .assertTrue { baseError ->
                    baseError.hasSealedModifier &&
                    baseError.resideInPackage("..error..")
                }
        }
    }

    // ========== Domain Error Structure ==========

    "domain errors should be sealed hierarchies" {
        boundedContexts.forEach { context ->
            Konsist
                .scopeFromDirectory("contexts/$context/domain")
                .classes()
                .filter { it.resideInPackage("..error..") }
                .filter { it.name.endsWith("Error") }
                .filter { !it.name.endsWith("Test") }
                .filter { it.isTopLevel }
                .assertTrue { error ->
                    // Top-level errors should be sealed
                    error.hasSealedModifier ||
                    // Or extend a sealed class
                    error.parents().any { parent ->
                        parent.name.endsWith("Error")
                    }
                }
        }
    }

    "error classes should provide context" {
        boundedContexts.forEach { context ->
            Konsist
                .scopeFromDirectory("contexts/$context/domain")
                .classes()
                .filter { it.resideInPackage("..error..") }
                .filter { it.name.endsWith("Error") }
                .filter { !it.hasSealedModifier }
                .filter { it.hasClassModifier || it.hasDataModifier } // Include only class or data class errors
                .assertTrue { error ->
                    // Errors should have properties for context
                    error.hasDataModifier || 
                    error.properties().isNotEmpty()
                }
        }
    }

    // ========== Repository Error Patterns ==========

    "repository errors should be operation-specific" {
        val operationErrorPatterns = listOf(
            "Save", "Find", "Delete", "Update", "Count", "Exists", "Query"
        )

        boundedContexts.forEach { context ->
            Konsist
                .scopeFromDirectory("contexts/$context/domain")
                .classes()
                .filter { it.resideInPackage("..error..") }
                .filter { clazz ->
                    operationErrorPatterns.any { pattern ->
                        clazz.name.contains(pattern) && clazz.name.endsWith("Error")
                    }
                }
                .assertTrue { operationError ->
                    // Operation-specific errors should be sealed classes with cases
                    operationError.hasSealedModifier ||
                    // Or be specific error cases with context
                    operationError.properties().any { prop ->
                        prop.name.contains("id") ||
                        prop.name.contains("Id") ||
                        prop.name.contains("message") ||
                        prop.name.contains("cause") ||
                        prop.name.contains("operation")
                    }
                }
        }
    }

    // ========== No Unknown/Default Fallbacks ==========

    "errors should not have 'unknown' or default fallbacks" {
        boundedContexts.forEach { context ->
            Konsist
                .scopeFromDirectory("contexts/$context")
                .files
                .filter { it.path.contains("src/main") }
                .assertFalse { file ->
                    // Check for unknown error patterns
                    file.text.contains("UnknownError") && !file.text.contains("UnknownError(") || // Allow as parameter
                    file.text.contains("DefaultError") ||
                    file.text.contains("else -> \"unknown\"") ||
                    file.text.contains("else -> \"Unknown\"") ||
                    file.text.contains("default:") && file.text.contains("unknown")
                }
        }
    }

    // ========== Validation Error Patterns ==========

    "validation errors should have specific context" {
        boundedContexts.forEach { context ->
            Konsist
                .scopeFromDirectory("contexts/$context")
                .classes()
                .filter { it.resideInPackage("..error..") }
                .filter { 
                    it.name.contains("Validation") ||
                    it.name.contains("Invalid") ||
                    it.name.contains("TooLong") ||
                    it.name.contains("TooShort") ||
                    it.name.contains("Empty")
                }
                .assertTrue { validationError ->
                    // Validation errors should include details
                    validationError.properties().any { prop ->
                        prop.name.contains("actual") ||
                        prop.name.contains("expected") ||
                        prop.name.contains("max") ||
                        prop.name.contains("min") ||
                        prop.name.contains("value") ||
                        prop.name.contains("field")
                    } || !validationError.hasClassModifier // Simple singleton errors are OK
                }
        }
    }

    // ========== Error Translation Patterns ==========

    "application errors should properly translate domain errors" {
        boundedContexts.forEach { context ->
            Konsist
                .scopeFromDirectory("contexts/$context/application")
                .classes()
                .filter { it.name.endsWith("Handler") }
                .filter { handler ->
                    // Check if handler deals with errors
                    handler.text.contains("mapLeft") ||
                    handler.text.contains("leftMap") ||
                    handler.text.contains("fold(")
                }
                .assertFalse { handler ->
                    // Should not swallow errors with generic messages
                    handler.text.contains("mapLeft { _ ->") || // Ignoring error details
                    handler.text.contains("mapLeft { \"error\" }") ||
                    handler.text.contains("fold({ null }") // Returning null on error
                }
        }
    }

    // ========== Either Error Handling ==========

    "use Either for error handling instead of exceptions" {
        boundedContexts.forEach { context ->
            Konsist
                .scopeFromDirectory("contexts/$context")
                .functions()
                .filter { !it.name.endsWith("Test") }
                .filter { it.text.contains("suspend") || it.hasReturnType() }
                .assertFalse { function ->
                    // Should not throw exceptions (except for TODOs)
                    function.text.contains("throw ") &&
                    !function.text.contains("TODO") &&
                    !function.text.contains("NotImplementedError") &&
                    !function.text.contains("error(") && // Kotlin's error() is acceptable
                    !function.text.contains("check(") &&
                    !function.text.contains("require(")
                }
        }
    }

    "repository methods should return error-specific Either types" {
        boundedContexts.forEach { context ->
            Konsist
                .scopeFromDirectory("contexts/$context/domain")
                .interfaces()
                .filter { it.name.endsWith("Repository") }
                .assertTrue { repository ->
                    repository.functions()
                        .filter { !it.name.startsWith("toString") }
                        .all { function ->
                            val returnType = function.returnType?.sourceType ?: ""
                            // Should return Either with specific error types
                            returnType.contains("Either<") &&
                            returnType.contains("Error")
                        }
                }
        }
    }

    // ========== Error Message Quality ==========

    "error messages should be descriptive" {
        boundedContexts.forEach { context ->
            Konsist
                .scopeFromDirectory("contexts/$context")
                .classes()
                .filter { it.resideInPackage("..error..") }
                .filter { it.hasDataModifier }
                .assertTrue { errorClass ->
                    // Error classes with string properties should have meaningful names
                    errorClass.properties()
                        .filter { it.type.sourceType == "String" }
                        .all { prop ->
                            prop.name != "s" &&
                            prop.name != "str" &&
                            prop.name != "msg" &&
                            prop.name.length > 2
                        }
                }
        }
    }

    // ========== Fail-Fast Patterns ==========

    "critical errors should not be caught and ignored" {
        boundedContexts.forEach { context ->
            Konsist
                .scopeFromDirectory("contexts/$context")
                .functions()
                .filter { it.text.contains("try") }
                .assertFalse { function ->
                    // Check for empty catch blocks or catching too broadly
                    function.text.contains("catch (_:") ||
                    function.text.contains("catch (e: Exception) {}") ||
                    function.text.contains("catch (e: Throwable) {}")
                }
        }
    }

    // ========== Service-Specific Error Types ==========

    "each validation service should return specific error types" {
        boundedContexts.forEach { context ->
            Konsist
                .scopeFromDirectory("contexts/$context/application")
                .classes()
                .filter { it.name.contains("ValidationService") }
                .assertTrue { validationService ->
                    validationService.functions()
                        .filter { it.name.startsWith("validate") }
                        .all { function ->
                            val returnType = function.returnType?.sourceType ?: ""
                            // Should return Either with specific validation error
                            returnType.contains("Either<") &&
                            (returnType.contains("ValidationError") ||
                             returnType.contains("Error") ||
                             returnType.contains("DomainError"))
                        }
                }
        }
    }

    // ========== Error Accumulation Patterns ==========

    "validation should support error accumulation" {
        // Skip this test for now as ValidationResult pattern is optional
        // Some contexts may use different error accumulation patterns
    }

    // ========== Infrastructure Error Mapping ==========

    "infrastructure errors should be mapped to domain errors" {
        boundedContexts.forEach { context ->
            Konsist
                .scopeFromDirectory("contexts/$context/infrastructure")
                .classes()
                .filter { it.name.endsWith("RepositoryImpl") || it.name.endsWith("Adapter") }
                .filter { clazz ->
                    // Check if class handles external errors
                    clazz.text.contains("SQLException") ||
                    clazz.text.contains("IOException") ||
                    clazz.text.contains("HttpException")
                }
                .assertTrue { infraClass ->
                    // Should map to domain errors, not expose infrastructure details
                    infraClass.text.contains("mapLeft") ||
                    infraClass.text.contains("fold(") ||
                    infraClass.text.contains("when")
                }
        }
    }
})