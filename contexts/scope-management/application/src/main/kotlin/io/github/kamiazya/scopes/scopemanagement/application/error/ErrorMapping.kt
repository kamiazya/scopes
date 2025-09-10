package io.github.kamiazya.scopes.scopemanagement.application.error

import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError as DomainContextError

/**
 * Maps ApplicationError to ScopesError for use in command/query handlers.
 * This provides a type-safe way to convert between error hierarchies
 * when crossing layer boundaries.
 */
fun ApplicationError.toScopesError(): ScopesError = when (this) {
    // PersistenceError is already a ScopesError subtype
    is PersistenceError -> this
    
    // ContextError from domain is already a ScopesError subtype through ContextManagementError
    is io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError -> this
    
    // Application-level context errors map to validation failures
    is ContextError -> when (this) {
        is ContextError.EmptyKey,
        is ContextError.KeyTooShort,
        is ContextError.KeyTooLong,
        is ContextError.KeyInvalidFormat -> ScopesError.ValidationFailed(
            field = "key",
            value = when (this) {
                is ContextError.KeyInvalidFormat -> attemptedKey
                else -> ""
            },
            constraint = ScopesError.ValidationConstraintType.InvalidFormat("context key format"),
            details = mapOf("error" to this.toString())
        )
        
        is ContextError.EmptyDescription,
        is ContextError.DescriptionTooShort,
        is ContextError.DescriptionTooLong -> ScopesError.ValidationFailed(
            field = "description",
            value = "",
            constraint = when (this) {
                is ContextError.DescriptionTooShort -> 
                    ScopesError.ValidationConstraintType.InvalidValue("Too short (minimum: $minimumLength)")
                is ContextError.DescriptionTooLong -> 
                    ScopesError.ValidationConstraintType.InvalidValue("Too long (maximum: $maximumLength)")
                else -> ScopesError.ValidationConstraintType.InvalidValue("Invalid description")
            }
        )
        
        is ContextError.EmptyFilter,
        is ContextError.FilterTooShort,
        is ContextError.FilterTooLong,
        is ContextError.FilterSyntaxError -> ScopesError.ValidationFailed(
            field = "filter",
            value = when (this) {
                is ContextError.FilterSyntaxError -> expression
                else -> ""
            },
            constraint = when (this) {
                is ContextError.FilterSyntaxError -> 
                    ScopesError.ValidationConstraintType.InvalidFormat("Invalid filter syntax: $message")
                is ContextError.FilterTooShort ->
                    ScopesError.ValidationConstraintType.InvalidValue("Too short (minimum: $minimumLength)")
                is ContextError.FilterTooLong ->
                    ScopesError.ValidationConstraintType.InvalidValue("Too long (maximum: $maximumLength)")
                else -> ScopesError.ValidationConstraintType.InvalidValue("Invalid filter")
            }
        )
        
        is ContextError.StateNotFound -> ScopesError.NotFound(
            entityType = "ContextView",
            identifier = contextId,
            identifierType = "id"
        )
        
        is ContextError.AlreadyExists -> ScopesError.AlreadyExists(
            entityType = "ContextView",
            identifier = key,
            identifierType = "key"
        )
        
        is ContextError.InvalidContextSwitch -> ScopesError.InvalidOperation(
            operation = "switch_context",
            entityType = "ContextView",
            entityId = key,
            reason = ScopesError.InvalidOperation.InvalidOperationReason.INVALID_STATE
        )
    }
    
    // Generic application errors
    else -> ScopesError.SystemError(
        errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
        service = "ApplicationService",
        context = mapOf("error" to this.toString())
    )
}

/**
 * Extension function for domain ContextError which is already a ScopesError subtype.
 * This is used when working with domain value objects that return ContextError.
 */
fun DomainContextError.toScopesError(): ScopesError = this