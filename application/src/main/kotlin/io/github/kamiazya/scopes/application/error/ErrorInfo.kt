package io.github.kamiazya.scopes.application.error

/**
 * Type-safe structured error information for presentation layer.
 * This sealed class hierarchy provides compile-time safety for error handling,
 * containing only structured data without presentation-specific messages.
 * The presentation layer is responsible for generating user-facing messages.
 */
sealed class ApplicationError(
    open val recoverable: Boolean = true
) {

    // Scope Input Errors - flattened to 2 levels
    sealed class ScopeInputError(recoverable: Boolean = true) : ApplicationError(recoverable) {
        data class IdBlank(val attemptedValue: String) : ScopeInputError()
        data class IdInvalidFormat(
            val attemptedValue: String,
            val expectedFormat: String = "ULID"
        ) : ScopeInputError()

        data class TitleEmpty(val attemptedValue: String) : ScopeInputError()
        data class TitleTooShort(
            val attemptedValue: String,
            val minimumLength: Int
        ) : ScopeInputError()
        data class TitleTooLong(
            val attemptedValue: String,
            val maximumLength: Int
        ) : ScopeInputError()
        data class TitleContainsProhibitedCharacters(
            val attemptedValue: String,
            val prohibitedCharacters: List<Char>
        ) : ScopeInputError()

        data class DescriptionTooLong(
            val attemptedValue: String,
            val maximumLength: Int
        ) : ScopeInputError()

        // Alias validation errors - structured like other errors
        data class AliasEmpty(val attemptedValue: String) : ScopeInputError()
        data class AliasTooShort(
            val attemptedValue: String,
            val minimumLength: Int
        ) : ScopeInputError()
        data class AliasTooLong(
            val attemptedValue: String,
            val maximumLength: Int
        ) : ScopeInputError()
        data class AliasInvalidFormat(
            val attemptedValue: String,
            val expectedPattern: String
        ) : ScopeInputError()
    }

    // Aspect Errors - flattened to 2 levels
    sealed class AspectError(recoverable: Boolean = true) : ApplicationError(recoverable) {
        data object KeyEmpty : AspectError()
        data class KeyInvalidFormat(
            val attemptedKey: String,
            val expectedPattern: String
        ) : AspectError()
        data class KeyReserved(val attemptedKey: String) : AspectError()

        data class ValueEmpty(val aspectKey: String) : AspectError()
        data class ValueNotInAllowedValues(
            val aspectKey: String,
            val attemptedValue: String,
            val allowedValues: List<String>
        ) : AspectError()
    }

    // Context Errors - flattened to 2 levels
    sealed class ContextError(recoverable: Boolean = true) : ApplicationError(recoverable) {
        data object NamingEmpty : ContextError()
        data class NamingAlreadyExists(val attemptedName: String) : ContextError()
        data class NamingInvalidFormat(val attemptedName: String) : ContextError()

        data class FilterInvalidSyntax(
            val position: Int,
            val reason: String,
            val expression: String
        ) : ContextError()
        data class FilterUnknownAspect(
            val unknownAspectKey: String,
            val expression: String
        ) : ContextError()
        data class FilterLogicalInconsistency(
            val reason: String,
            val expression: String
        ) : ContextError()

        data class StateNotFound(
            val contextName: String? = null,
            val contextId: String? = null
        ) : ContextError()
        data class StateFilterProducesNoResults(
            val contextName: String,
            val filterExpression: String
        ) : ContextError()
        data class ActiveContextDeleteAttempt(
            val contextId: String
        ) : ContextError()
    }

    // Scope Hierarchy Errors
    sealed class ScopeHierarchyError(recoverable: Boolean = true) : ApplicationError(recoverable) {
        data class CircularReference(
            val scopeId: String,
            val cyclePath: List<String>
        ) : ScopeHierarchyError(false)

        data class MaxDepthExceeded(
            val scopeId: String,
            val attemptedDepth: Int,
            val maximumDepth: Int
        ) : ScopeHierarchyError()

        data class MaxChildrenExceeded(
            val parentScopeId: String,
            val currentChildrenCount: Int,
            val maximumChildren: Int
        ) : ScopeHierarchyError()

        data class SelfParenting(val scopeId: String) : ScopeHierarchyError()

        data class ParentNotFound(
            val scopeId: String,
            val parentId: String
        ) : ScopeHierarchyError()

        data class InvalidParentId(val invalidId: String) : ScopeHierarchyError()
    }

    // Scope Uniqueness Errors
    sealed class ScopeUniquenessError(recoverable: Boolean = true) : ApplicationError(recoverable) {
        data class DuplicateTitle(
            val title: String,
            val parentScopeId: String?,
            val existingScopeId: String
        ) : ScopeUniquenessError()
    }

    // Persistence Errors
    sealed class PersistenceError(recoverable: Boolean = false) : ApplicationError(recoverable) {
        data class StorageUnavailable(
            val operation: String,
            val cause: String?
        ) : PersistenceError()

        data class DataCorruption(
            val entityType: String,
            val entityId: String?,
            val reason: String
        ) : PersistenceError()

        data class ConcurrencyConflict(
            val entityType: String,
            val entityId: String,
            val expectedVersion: String,
            val actualVersion: String
        ) : PersistenceError()
    }

    // External System Errors
    sealed class ExternalSystemError(recoverable: Boolean = false) : ApplicationError(recoverable) {
        data class ServiceUnavailable(
            val serviceName: String,
            val operation: String
        ) : ExternalSystemError()

        data class AuthenticationFailed(val serviceName: String) : ExternalSystemError()
    }

    // Scope Alias Errors
    sealed class ScopeAliasError(recoverable: Boolean = true) : ApplicationError(recoverable) {
        data class DuplicateAlias(
            val aliasName: String,
            val existingScopeId: String,
            val attemptedScopeId: String
        ) : ScopeAliasError()

        data class AliasNotFound(
            val aliasName: String
        ) : ScopeAliasError()

        data class CannotRemoveCanonicalAlias(
            val scopeId: String,
            val aliasName: String
        ) : ScopeAliasError()

        data class AliasGenerationFailed(
            val scopeId: String,
            val retryCount: Int
        ) : ScopeAliasError()
    }
}

/**
 * Service for converting domain errors to type-safe presentation-friendly error information.
 * This maintains the separation between domain and presentation concerns.
 */
interface ErrorInfoService {
    /**
     * Convert any domain error to structured error information.
     */
    fun toErrorInfo(error: Any): ApplicationError
}

/**
 * Service for formatting error information into user-readable messages.
 * Different presentation layers can provide their own implementations.
 */
interface ErrorMessageFormatter {
    /**
     * Format error information into a user-readable message.
     */
    fun format(errorInfo: ApplicationError): String
}

