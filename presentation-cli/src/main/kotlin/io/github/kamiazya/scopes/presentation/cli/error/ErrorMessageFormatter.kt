package io.github.kamiazya.scopes.presentation.cli.error

import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.error.ScopeInputError
import io.github.kamiazya.scopes.application.error.AspectError
import io.github.kamiazya.scopes.application.error.ContextError
import io.github.kamiazya.scopes.application.error.ScopeHierarchyError
import io.github.kamiazya.scopes.application.error.ScopeUniquenessError
import io.github.kamiazya.scopes.application.error.PersistenceError
import io.github.kamiazya.scopes.application.error.ExternalSystemError
import io.github.kamiazya.scopes.application.error.ScopeAliasError
import io.github.kamiazya.scopes.application.error.ErrorMessageFormatter as AppErrorMessageFormatter

/**
 * CLI-specific formatter for converting type-safe structured error information to user-readable CLI messages.
 * This is part of the presentation layer and handles CLI-specific error message formatting.
 *
 * Following Clean Architecture principles, this formatter works with type-safe structured error information
 * from the application layer, providing compile-time safety and generating presentation-specific messages.
 * Different presentation layers (Web API, GUI) would have their own formatters with different message styles.
 */
object CliErrorMessageFormatter : AppErrorMessageFormatter {

    /**
     * Convert type-safe structured error information to a user-readable CLI message.
     * The sealed class hierarchy provides compile-time safety for all error types.
     */
    override fun format(errorInfo: ApplicationError): String = buildString {
        // Generate CLI-appropriate messages based on error type
        when (errorInfo) {
            // Scope Input Errors - updated for flattened structure
            is ScopeInputError.IdBlank -> {
                append("Scope ID cannot be empty. Please provide a valid ULID.")
            }
            is ScopeInputError.IdInvalidFormat -> {
                append("Scope ID '${errorInfo.attemptedValue}' has invalid format. ")
                append("Expected format: ${errorInfo.expectedFormat}")
            }

            is ScopeInputError.TitleEmpty -> {
                append("Scope title cannot be empty. Please provide a meaningful title.")
            }
            is ScopeInputError.TitleTooShort -> {
                append("Scope title '${errorInfo.attemptedValue}' is too short. ")
                append("Minimum length is ${errorInfo.minimumLength} characters.")
            }
            is ScopeInputError.TitleTooLong -> {
                append("Scope title '${errorInfo.attemptedValue}' is too long. ")
                append("Maximum length is ${errorInfo.maximumLength} characters.")
            }
            is ScopeInputError.TitleContainsProhibitedCharacters -> {
                append("Scope title '${errorInfo.attemptedValue}' contains prohibited characters: ")
                append("${errorInfo.prohibitedCharacters.joinToString(", ")}. ")
                append("Please use only allowed characters.")
            }

            is ScopeInputError.DescriptionTooLong -> {
                append("Scope description is too long. Maximum length is ${errorInfo.maximumLength} characters.")
            }

            is ScopeInputError.AliasEmpty -> {
                append("Alias cannot be empty. Please provide a valid alias.")
            }
            is ScopeInputError.AliasTooShort -> {
                append("Alias '${errorInfo.attemptedValue}' is too short. ")
                append("Minimum length is ${errorInfo.minimumLength} characters.")
            }
            is ScopeInputError.AliasTooLong -> {
                append("Alias '${errorInfo.attemptedValue}' is too long. ")
                append("Maximum length is ${errorInfo.maximumLength} characters.")
            }
            is ScopeInputError.AliasInvalidFormat -> {
                append("Alias '${errorInfo.attemptedValue}' has invalid format. ")
                append("Expected pattern: ${errorInfo.expectedPattern}")
            }

            // Aspect Errors - updated for flattened structure
            is AspectError.KeyEmpty -> {
                append("Aspect key cannot be empty.")
            }
            is AspectError.KeyInvalidFormat -> {
                append("Aspect key '${errorInfo.attemptedKey}' has invalid format. ")
                append("Expected pattern: ${errorInfo.expectedPattern}")
            }
            is AspectError.KeyReserved -> {
                append("Aspect key '${errorInfo.attemptedKey}' is reserved and cannot be used.")
            }

            is AspectError.ValueEmpty -> {
                append("Value for aspect '${errorInfo.aspectKey}' cannot be empty.")
            }
            is AspectError.ValueNotInAllowedValues -> {
                append("Value '${errorInfo.attemptedValue}' for aspect '${errorInfo.aspectKey}' is not allowed. ")
                append("Allowed values: ${errorInfo.allowedValues.joinToString(", ")}")
            }

            // Context Errors - updated for flattened structure
            is ContextError.NamingEmpty -> {
                append("Context name cannot be empty.")
            }
            is ContextError.NamingAlreadyExists -> {
                append("Context with name '${errorInfo.attemptedName}' already exists.")
            }
            is ContextError.NamingInvalidFormat -> {
                append("Context name '${errorInfo.attemptedName}' has invalid format.")
            }

            is ContextError.FilterInvalidSyntax -> {
                append("Invalid filter syntax at position ${errorInfo.position}: ${errorInfo.reason}")
                append("\nExpression: ${errorInfo.expression}")
            }
            is ContextError.FilterUnknownAspect -> {
                append("Unknown aspect '${errorInfo.unknownAspectKey}' in filter expression.")
                append("\nExpression: ${errorInfo.expression}")
            }
            is ContextError.FilterLogicalInconsistency -> {
                append("Logical inconsistency in filter expression: ${errorInfo.reason}")
                append("\nExpression: ${errorInfo.expression}")
            }

            is ContextError.StateNotFound -> {
                when {
                    errorInfo.contextName != null -> append("Context '${errorInfo.contextName}' not found.")
                    errorInfo.contextId != null -> append("Context with ID '${errorInfo.contextId}' not found.")
                    else -> append("Context not found.")
                }
            }
            is ContextError.StateFilterProducesNoResults -> {
                append("Filter for context '${errorInfo.contextName}' produces no results.")
                append("\nFilter expression: ${errorInfo.filterExpression}")
            }
            is ContextError.ActiveContextDeleteAttempt -> {
                append("Cannot delete the currently active context (ID: '${errorInfo.contextId}').")
                append("\nPlease switch to a different context before deleting this one.")
            }

            // Scope Hierarchy Errors
            is ScopeHierarchyError.CircularReference -> {
                append("Circular reference detected: Cannot set parent that would create a cycle.")
                append("\nCycle path: ${errorInfo.cyclePath.joinToString(" -> ")}")
            }
            is ScopeHierarchyError.MaxDepthExceeded -> {
                append("Maximum hierarchy depth exceeded. ")
                append("Attempted depth: ${errorInfo.attemptedDepth}, ")
                append("Maximum allowed: ${errorInfo.maximumDepth}")
            }
            is ScopeHierarchyError.MaxChildrenExceeded -> {
                append("Maximum children exceeded for parent scope. ")
                append("Current count: ${errorInfo.currentChildrenCount}, ")
                append("Maximum allowed: ${errorInfo.maximumChildren}")
            }
            is ScopeHierarchyError.SelfParenting -> {
                append("A scope cannot be its own parent.")
            }
            is ScopeHierarchyError.ParentNotFound -> {
                append("Parent scope not found: ${errorInfo.parentId}")
            }
            is ScopeHierarchyError.InvalidParentId -> {
                append("Invalid parent ID format: '${errorInfo.invalidId}'. Please provide a valid ULID format.")
            }

            // Scope Uniqueness Errors
            is ScopeUniquenessError.DuplicateTitle -> {
                append("A scope with title '${errorInfo.title}' already exists ")
                if (errorInfo.parentScopeId != null) {
                    append("under the same parent.")
                } else {
                    append("at the root level.")
                }
            }

            // Persistence Errors
            is PersistenceError.StorageUnavailable -> {
                append("Storage unavailable during operation '${errorInfo.operation}'.")
                errorInfo.cause?.let { cause ->
                    append("\nCause: $cause")
                }
            }
            is PersistenceError.DataCorruption -> {
                append("Data corruption detected in ${errorInfo.entityType}")
                errorInfo.entityId?.let { id ->
                    append(" (ID: $id)")
                }
                append(": ${errorInfo.reason}")
            }
            is PersistenceError.ConcurrencyConflict -> {
                append("Concurrency conflict for ${errorInfo.entityType} (ID: ${errorInfo.entityId}). ")
                append("Expected version: ${errorInfo.expectedVersion}, ")
                append("Actual version: ${errorInfo.actualVersion}")
            }

            // External System Errors
            is ExternalSystemError.ServiceUnavailable -> {
                append("Service '${errorInfo.serviceName}' is unavailable for operation '${errorInfo.operation}'.")
            }
            is ExternalSystemError.AuthenticationFailed -> {
                append("Authentication failed for service '${errorInfo.serviceName}'.")
            }

            // Scope Alias Errors
            is ScopeAliasError.DuplicateAlias -> {
                append("Alias '${errorInfo.aliasName}' is already assigned to scope ${errorInfo.existingScopeId}.")
            }
            is ScopeAliasError.AliasNotFound -> {
                append("Alias '${errorInfo.aliasName}' not found.")
            }
            is ScopeAliasError.CannotRemoveCanonicalAlias -> {
                append("Cannot remove canonical alias '${errorInfo.aliasName}' from scope ${errorInfo.scopeId}.")
            }
            is ScopeAliasError.AliasGenerationFailed -> {
                append("Failed to generate alias for scope ${errorInfo.scopeId} after ${errorInfo.retryCount} attempts.")
            }
        }

        // Add recovery suggestions based on error recoverability
        if (errorInfo.recoverable) {
            append("\nPlease review and correct your input.")
        } else {
            append("\nThis is a technical issue. Please contact support if the problem persists.")
        }
    }
}
