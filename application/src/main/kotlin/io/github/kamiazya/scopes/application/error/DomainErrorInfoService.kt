package io.github.kamiazya.scopes.application.error

import io.github.kamiazya.scopes.domain.error.*

/**
 * Implementation of ErrorInfoService that converts domain errors to type-safe structured error information.
 * This service maintains the boundary between domain and presentation layers with compile-time safety.
 */
class DomainErrorInfoService : ErrorInfoService {

    override fun toErrorInfo(error: Any): ApplicationError = when (error) {
        is ScopesError -> mapDomainError(error)
        else -> ApplicationError.ExternalSystemError.ServiceUnavailable("Unknown", "error_conversion")
    }

    private fun mapDomainError(error: ScopesError): ApplicationError = when (error) {
        is UserIntentionError -> mapUserIntentionError(error)
        is ConceptualModelError -> mapConceptualModelError(error)
        is InfrastructuralError -> mapInfrastructuralError(error)
    }

    private fun mapUserIntentionError(error: UserIntentionError): ApplicationError = when (error) {
        is ScopeInputError -> mapScopeInputError(error)
        is AspectError -> mapAspectError(error)
        is ContextError -> mapContextError(error)
    }

    private fun mapScopeInputError(error: ScopeInputError): ApplicationError = when (error) {
        is ScopeInputError.IdError -> mapIdError(error)
        is ScopeInputError.TitleError -> mapTitleError(error)
        is ScopeInputError.DescriptionError -> mapDescriptionError(error)
    }

    private fun mapIdError(error: ScopeInputError.IdError): ApplicationError = when (error) {
        is ScopeInputError.IdError.Blank -> ApplicationError.ScopeInputError.IdBlank(error.attemptedValue)
        is ScopeInputError.IdError.InvalidFormat -> ApplicationError.ScopeInputError.IdInvalidFormat(
            error.attemptedValue,
            error.expectedFormat
        )
    }

    private fun mapTitleError(error: ScopeInputError.TitleError): ApplicationError = when (error) {
        is ScopeInputError.TitleError.Empty -> ApplicationError.ScopeInputError.TitleEmpty(error.attemptedValue)
        is ScopeInputError.TitleError.TooShort -> ApplicationError.ScopeInputError.TitleTooShort(
            error.attemptedValue,
            error.minimumLength
        )
        is ScopeInputError.TitleError.TooLong -> ApplicationError.ScopeInputError.TitleTooLong(
            error.attemptedValue,
            error.maximumLength
        )
        is ScopeInputError.TitleError.ContainsProhibitedCharacters -> ApplicationError.ScopeInputError.TitleContainsProhibitedCharacters(
            error.attemptedValue,
            error.prohibitedCharacters
        )
    }

    private fun mapDescriptionError(error: ScopeInputError.DescriptionError): ApplicationError = when (error) {
        is ScopeInputError.DescriptionError.TooLong -> ApplicationError.ScopeInputError.DescriptionTooLong(
            error.attemptedValue,
            error.maximumLength
        )
    }

    private fun mapAspectError(error: AspectError): ApplicationError = when (error) {
        is AspectError.KeyError -> mapAspectKeyError(error)
        is AspectError.ValueError -> mapAspectValueError(error)
    }

    private fun mapAspectKeyError(error: AspectError.KeyError): ApplicationError = when (error) {
        is AspectError.KeyError.Empty -> ApplicationError.AspectError.KeyEmpty
        is AspectError.KeyError.InvalidFormat -> ApplicationError.AspectError.KeyInvalidFormat(
            error.attemptedKey,
            error.expectedPattern
        )
        is AspectError.KeyError.Reserved -> ApplicationError.AspectError.KeyReserved(error.attemptedKey)
    }

    private fun mapAspectValueError(error: AspectError.ValueError): ApplicationError = when (error) {
        is AspectError.ValueError.Empty -> ApplicationError.AspectError.ValueEmpty(error.aspectKey.toString())
        is AspectError.ValueError.NotInAllowedValues -> ApplicationError.AspectError.ValueNotInAllowedValues(
            error.aspectKey.toString(),
            error.attemptedValue,
            error.allowedValues
        )
    }

    private fun mapContextError(error: ContextError): ApplicationError = when (error) {
        is ContextError.NamingError -> mapContextNamingError(error)
        is ContextError.FilterError -> mapContextFilterError(error)
    }

    private fun mapContextNamingError(error: ContextError.NamingError): ApplicationError = when (error) {
        is ContextError.NamingError.Empty -> ApplicationError.ContextError.NamingEmpty
        is ContextError.NamingError.AlreadyExists -> ApplicationError.ContextError.NamingAlreadyExists(error.attemptedName)
        is ContextError.NamingError.InvalidFormat -> ApplicationError.ContextError.NamingInvalidFormat(error.attemptedName)
    }

    private fun mapContextFilterError(error: ContextError.FilterError): ApplicationError = when (error) {
        is ContextError.FilterError.InvalidSyntax -> ApplicationError.ContextError.FilterInvalidSyntax(
            error.position,
            error.reason,
            error.expression
        )
        is ContextError.FilterError.UnknownAspect -> ApplicationError.ContextError.FilterUnknownAspect(
            error.unknownAspectKey,
            error.expression
        )
        is ContextError.FilterError.LogicalInconsistency -> ApplicationError.ContextError.FilterLogicalInconsistency(
            error.reason,
            error.expression
        )
    }

    private fun mapConceptualModelError(error: ConceptualModelError): ApplicationError = when (error) {
        is ScopeHierarchyError -> mapScopeHierarchyError(error)
        is ScopeUniquenessError -> mapScopeUniquenessError(error)
        is ContextStateError -> mapContextStateError(error)
    }

    private fun mapScopeHierarchyError(error: ScopeHierarchyError): ApplicationError = when (error) {
        is ScopeHierarchyError.CircularReference -> ApplicationError.ScopeHierarchyError.CircularReference(
            error.scopeId.value,
            error.cyclePath.map { it.value }
        )
        is ScopeHierarchyError.MaxDepthExceeded -> ApplicationError.ScopeHierarchyError.MaxDepthExceeded(
            error.scopeId.value,
            error.attemptedDepth,
            error.maximumDepth
        )
        is ScopeHierarchyError.MaxChildrenExceeded -> ApplicationError.ScopeHierarchyError.MaxChildrenExceeded(
            error.parentScopeId.value,
            error.currentChildrenCount,
            error.maximumChildren
        )
        is ScopeHierarchyError.SelfParenting -> ApplicationError.ScopeHierarchyError.SelfParenting(error.scopeId.value)
        is ScopeHierarchyError.ParentNotFound -> ApplicationError.ScopeHierarchyError.ParentNotFound(
            error.scopeId.value,
            error.parentId.value
        )
        is ScopeHierarchyError.InvalidParentId -> ApplicationError.ScopeHierarchyError.InvalidParentId(error.invalidId)
        is ScopeHierarchyError.TooDeep -> ApplicationError.ScopeHierarchyError.TooDeep(
            error.scopeId.value,
            error.maxDepth
        )
        is ScopeHierarchyError.TooManyChildren -> ApplicationError.ScopeHierarchyError.TooManyChildren(
            error.parentId.value,
            error.maxChildren
        )
    }

    private fun mapScopeUniquenessError(error: ScopeUniquenessError): ApplicationError = when (error) {
        is ScopeUniquenessError.DuplicateTitle -> ApplicationError.ScopeUniquenessError.DuplicateTitle(
            error.title,
            error.parentScopeId?.value,
            error.existingScopeId.value
        )
    }

    private fun mapContextStateError(error: ContextStateError): ApplicationError = when (error) {
        is ContextStateError.NotFound -> ApplicationError.ContextError.StateNotFound(
            error.contextName,
            error.contextId?.value
        )
        is ContextStateError.FilterProducesNoResults -> ApplicationError.ContextError.StateFilterProducesNoResults(
            error.contextName,
            error.filterExpression
        )
    }

    private fun mapInfrastructuralError(error: InfrastructuralError): ApplicationError = when (error) {
        is PersistenceError -> mapPersistenceError(error)
        is ExternalSystemError -> mapExternalSystemError(error)
    }

    private fun mapPersistenceError(error: PersistenceError): ApplicationError = when (error) {
        is PersistenceError.StorageUnavailable -> ApplicationError.PersistenceError.StorageUnavailable(
            error.operation,
            error.cause?.message
        )
        is PersistenceError.DataCorruption -> ApplicationError.PersistenceError.DataCorruption(
            error.entityType,
            error.entityId,
            error.reason
        )
        is PersistenceError.ConcurrencyConflict -> ApplicationError.PersistenceError.ConcurrencyConflict(
            error.entityType,
            error.entityId,
            error.expectedVersion,
            error.actualVersion
        )
    }

    private fun mapExternalSystemError(error: ExternalSystemError): ApplicationError = when (error) {
        is ExternalSystemError.ServiceUnavailable -> ApplicationError.ExternalSystemError.ServiceUnavailable(
            error.serviceName,
            error.operation
        )
        is ExternalSystemError.AuthenticationFailed -> ApplicationError.ExternalSystemError.AuthenticationFailed(error.serviceName)
    }
}