package io.github.kamiazya.scopes.application.error

import io.github.kamiazya.scopes.domain.error.*

/**
 * Maps domain-layer errors to application-layer errors.
 * This ensures Clean Architecture compliance by preventing domain error types
 * from leaking through the application layer to the presentation layer.
 */
object DomainErrorMapper {
    
    /**
     * Convert a domain error to an application error.
     * This mapping preserves the semantic meaning while abstracting domain details.
     */
    fun mapToApplicationError(domainError: ScopesError): ApplicationError {
        return when (domainError) {
            // Context errors
            is ContextError.NamingError.Empty -> 
                ApplicationError.ContextError.NamingEmpty
            
            is ContextError.NamingError.AlreadyExists ->
                ApplicationError.ContextError.NamingAlreadyExists(
                    attemptedName = domainError.attemptedName
                )
            
            is ContextError.NamingError.InvalidFormat ->
                ApplicationError.ContextError.NamingInvalidFormat(
                    attemptedName = domainError.attemptedName
                )
            
            is ContextError.FilterError.InvalidSyntax ->
                ApplicationError.ContextError.FilterInvalidSyntax(
                    position = domainError.position,
                    reason = domainError.reason,
                    expression = domainError.expression
                )
            
            is ContextError.FilterError.UnknownAspect ->
                ApplicationError.ContextError.FilterUnknownAspect(
                    unknownAspectKey = domainError.unknownAspectKey,
                    expression = domainError.expression
                )
            
            is ContextError.FilterError.LogicalInconsistency ->
                ApplicationError.ContextError.FilterLogicalInconsistency(
                    reason = domainError.reason,
                    expression = domainError.expression
                )
            
            is ContextStateError.NotFound ->
                ApplicationError.ContextError.StateNotFound(
                    contextName = domainError.contextName,
                    contextId = domainError.contextId?.value  // Convert ContextViewId to String
                )
            
            is ContextStateError.FilterProducesNoResults ->
                ApplicationError.ContextError.StateFilterProducesNoResults(
                    contextName = domainError.contextName,
                    filterExpression = domainError.filterExpression
                )
            
            // Persistence errors
            is PersistenceError.StorageUnavailable ->
                ApplicationError.PersistenceError.StorageUnavailable(
                    operation = domainError.operation,
                    cause = domainError.cause?.message
                )
            
            is PersistenceError.DataCorruption ->
                ApplicationError.PersistenceError.DataCorruption(
                    entityType = domainError.entityType,
                    entityId = domainError.entityId,
                    reason = domainError.reason
                )
            
            is PersistenceError.ConcurrencyConflict ->
                ApplicationError.PersistenceError.ConcurrencyConflict(
                    entityType = domainError.entityType,
                    entityId = domainError.entityId,
                    expectedVersion = domainError.expectedVersion.toString(),
                    actualVersion = domainError.actualVersion.toString()
                )
            
            // Scope input errors
            is ScopeInputError.IdError.Blank ->
                ApplicationError.ScopeInputError.IdBlank(
                    attemptedValue = ""  // Blank means empty
                )
            
            is ScopeInputError.IdError.InvalidFormat ->
                ApplicationError.ScopeInputError.IdInvalidFormat(
                    attemptedValue = domainError.attemptedValue,
                    expectedFormat = domainError.expectedFormat
                )
            
            is ScopeInputError.TitleError.Empty ->
                ApplicationError.ScopeInputError.TitleEmpty(
                    attemptedValue = ""  // Empty means empty string
                )
            
            is ScopeInputError.TitleError.TooShort ->
                ApplicationError.ScopeInputError.TitleTooShort(
                    attemptedValue = domainError.attemptedValue,
                    minimumLength = domainError.minimumLength
                )
            
            is ScopeInputError.TitleError.TooLong ->
                ApplicationError.ScopeInputError.TitleTooLong(
                    attemptedValue = domainError.attemptedValue,
                    maximumLength = domainError.maximumLength
                )
            
            is ScopeInputError.TitleError.ContainsProhibitedCharacters ->
                ApplicationError.ScopeInputError.TitleContainsProhibitedCharacters(
                    attemptedValue = domainError.attemptedValue,
                    prohibitedCharacters = domainError.prohibitedCharacters
                )
            
            is ScopeInputError.DescriptionError.TooLong ->
                ApplicationError.ScopeInputError.DescriptionTooLong(
                    attemptedValue = domainError.attemptedValue,
                    maximumLength = domainError.maximumLength
                )
            
            is ScopeInputError.AliasError.Empty ->
                ApplicationError.ScopeInputError.AliasEmpty(
                    attemptedValue = ""  // Empty means empty string
                )
            
            is ScopeInputError.AliasError.TooShort ->
                ApplicationError.ScopeInputError.AliasTooShort(
                    attemptedValue = domainError.attemptedValue,
                    minimumLength = domainError.minimumLength
                )
            
            is ScopeInputError.AliasError.TooLong ->
                ApplicationError.ScopeInputError.AliasTooLong(
                    attemptedValue = domainError.attemptedValue,
                    maximumLength = domainError.maximumLength
                )
            
            is ScopeInputError.AliasError.InvalidFormat ->
                ApplicationError.ScopeInputError.AliasInvalidFormat(
                    attemptedValue = domainError.attemptedValue,
                    expectedPattern = domainError.expectedPattern
                )
            
            // Aspect errors
            is AspectError.KeyError.Empty ->
                ApplicationError.AspectError.KeyEmpty
            
            is AspectError.KeyError.InvalidFormat ->
                ApplicationError.AspectError.KeyInvalidFormat(
                    attemptedKey = domainError.attemptedKey,
                    expectedPattern = domainError.expectedPattern
                )
            
            is AspectError.KeyError.Reserved ->
                ApplicationError.AspectError.KeyReserved(
                    attemptedKey = domainError.attemptedKey
                )
            
            is AspectError.ValueError.Empty ->
                ApplicationError.AspectError.ValueEmpty(
                    aspectKey = domainError.aspectKey
                )
            
            is AspectError.ValueError.NotInAllowedValues ->
                ApplicationError.AspectError.ValueNotInAllowedValues(
                    aspectKey = domainError.aspectKey,
                    attemptedValue = domainError.attemptedValue,
                    allowedValues = domainError.allowedValues
                )
            
            // Scope hierarchy errors
            is ScopeHierarchyError.CircularReference ->
                ApplicationError.ScopeHierarchyError.CircularReference(
                    scopeId = domainError.scopeId.value,
                    cyclePath = domainError.cyclePath.map { it.value }
                )
            
            is ScopeHierarchyError.MaxDepthExceeded ->
                ApplicationError.ScopeHierarchyError.MaxDepthExceeded(
                    scopeId = domainError.scopeId.value,
                    attemptedDepth = domainError.attemptedDepth,
                    maximumDepth = domainError.maximumDepth
                )
            
            is ScopeHierarchyError.MaxChildrenExceeded ->
                ApplicationError.ScopeHierarchyError.MaxChildrenExceeded(
                    parentScopeId = domainError.parentScopeId.value,
                    currentChildrenCount = domainError.currentChildrenCount,
                    maximumChildren = domainError.maximumChildren
                )
            
            is ScopeHierarchyError.SelfParenting ->
                ApplicationError.ScopeHierarchyError.SelfParenting(
                    scopeId = domainError.scopeId.value
                )
            
            is ScopeHierarchyError.ParentNotFound ->
                ApplicationError.ScopeHierarchyError.ParentNotFound(
                    scopeId = domainError.scopeId.value,
                    parentId = domainError.parentId.value
                )
            
            is ScopeHierarchyError.InvalidParentId ->
                ApplicationError.ScopeHierarchyError.InvalidParentId(
                    invalidId = domainError.invalidId
                )
            
            // Scope uniqueness errors
            is ScopeUniquenessError.DuplicateTitle ->
                ApplicationError.ScopeUniquenessError.DuplicateTitle(
                    title = domainError.title,
                    parentScopeId = domainError.parentScopeId?.value,
                    existingScopeId = domainError.existingScopeId.value
                )
            
            // Scope alias errors
            is ScopeAliasError.DuplicateAlias ->
                ApplicationError.ScopeAliasError.DuplicateAlias(
                    aliasName = domainError.aliasName,
                    existingScopeId = domainError.existingScopeId.value,
                    attemptedScopeId = domainError.attemptedScopeId.value
                )
            
            is ScopeAliasError.CanonicalAliasAlreadyExists ->
                // Map to a general duplicate alias error since the application layer
                // doesn't distinguish between canonical and custom aliases in its errors
                ApplicationError.ScopeAliasError.DuplicateAlias(
                    aliasName = domainError.existingCanonicalAlias,
                    existingScopeId = domainError.scopeId.value,
                    attemptedScopeId = domainError.scopeId.value  // Same scope attempting to add another canonical
                )
            
            is ScopeAliasError.AliasNotFound ->
                ApplicationError.ScopeAliasError.AliasNotFound(
                    aliasName = domainError.aliasName
                )
            
            is ScopeAliasError.CannotRemoveCanonicalAlias ->
                ApplicationError.ScopeAliasError.CannotRemoveCanonicalAlias(
                    scopeId = domainError.scopeId.value,
                    aliasName = domainError.canonicalAlias
                )
            
            // External system errors (if we have any domain external errors)
            is ExternalSystemError.ServiceUnavailable ->
                ApplicationError.ExternalSystemError.ServiceUnavailable(
                    serviceName = domainError.serviceName,
                    operation = domainError.operation
                )
            
            is ExternalSystemError.AuthenticationFailed ->
                ApplicationError.ExternalSystemError.AuthenticationFailed(
                    serviceName = domainError.serviceName
                )
        }
    }
}