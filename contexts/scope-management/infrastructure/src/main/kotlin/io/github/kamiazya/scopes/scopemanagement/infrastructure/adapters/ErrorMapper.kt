package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.error.BaseErrorMapper
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputErrorPresenter
import io.github.kamiazya.scopes.scopemanagement.domain.error.*

/**
 * Maps domain errors to contract layer errors.
 *
 * This mapper provides a stable error contract between bounded contexts,
 * ensuring that internal domain errors are properly translated to
 * contract-level errors that external consumers can understand.
 *
 * Key responsibilities:
 * - Translate domain-specific errors to contract errors
 * - Preserve error context and messages
 * - Maintain error hierarchy mapping
 * - Hide internal implementation details
 * - Log unmapped errors for visibility and debugging
 */
class ErrorMapper(logger: Logger) : BaseErrorMapper<ScopesError, ScopeContractError>(logger) {

    companion object {
        private const val SCOPE_MANAGEMENT_SERVICE = "scope-management"
    }
    private val errorPresenter = ScopeInputErrorPresenter()

    override fun getServiceName(): String = SCOPE_MANAGEMENT_SERVICE

    override fun createServiceUnavailableError(serviceName: String): ScopeContractError =
        ScopeContractError.SystemError.ServiceUnavailable(service = serviceName)

    override fun mapToContractError(domainError: ScopesError): ScopeContractError = when (domainError) {
        // Input validation errors
        is ScopeInputError.IdError -> mapIdError(domainError)
        is ScopeInputError.TitleError -> mapTitleError(domainError)
        is ScopeInputError.DescriptionError -> mapDescriptionError(domainError)
        is ScopeInputError.AliasError -> mapAliasError(domainError)

        // Business rule violations
        is ScopeError -> mapScopeError(domainError)
        is ScopeUniquenessError -> mapUniquenessError(domainError)
        is ScopeHierarchyError -> mapHierarchyError(domainError)
        is ScopeAliasError -> mapAliasErrorDomain(domainError)

        // New structured errors
        is ScopesError.NotFound -> mapNotFoundError(domainError)
        is ScopesError.AlreadyExists -> mapAlreadyExistsError(domainError)
        is ScopesError.SystemError -> mapSystemError(domainError)
        is ScopesError.ValidationFailed -> mapValidationError(domainError)
        is ScopesError.InvalidOperation -> mapInvalidOperationError(domainError)
        is ScopesError.Conflict -> mapConflictError(domainError)
        is ScopesError.ConcurrencyError -> ScopeContractError.SystemError.ConcurrentModification(
            scopeId = domainError.aggregateId,
            expectedVersion = domainError.expectedVersion?.toLong() ?: -1L,
            actualVersion = domainError.actualVersion?.toLong() ?: -1L,
        )
        is ScopesError.RepositoryError -> mapSystemError()

        // Default fallback for unmapped errors
        else -> handleUnmappedError(
            domainError,
            mapSystemError(),
        )
    }

    private fun mapIdError(domainError: ScopeInputError.IdError): ScopeContractError.InputError.InvalidId = when (domainError) {
        is ScopeInputError.IdError.EmptyId -> ScopeContractError.InputError.InvalidId(
            id = "",
            expectedFormat = "Non-empty ULID format",
        )
        is ScopeInputError.IdError.InvalidIdFormat -> ScopeContractError.InputError.InvalidId(
            id = domainError.id,
            expectedFormat = errorPresenter.presentIdFormat(domainError.expectedFormat),
        )
    }

    private fun mapTitleError(domainError: ScopeInputError.TitleError): ScopeContractError.InputError.InvalidTitle = when (domainError) {
        is ScopeInputError.TitleError.EmptyTitle -> ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.Empty,
        )
        is ScopeInputError.TitleError.TitleTooShort -> ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                minimumLength = domainError.minLength,
                actualLength = 0,
            ),
        )
        is ScopeInputError.TitleError.TitleTooLong -> ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                maximumLength = domainError.maxLength,
                actualLength = 0,
            ),
        )
        is ScopeInputError.TitleError.InvalidTitleFormat -> ScopeContractError.InputError.InvalidTitle(
            title = domainError.title,
            validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(
                prohibitedCharacters = emptyList(),
            ),
        )
    }

    private fun mapDescriptionError(domainError: ScopeInputError.DescriptionError): ScopeContractError.InputError.InvalidDescription = when (domainError) {
        is ScopeInputError.DescriptionError.DescriptionTooLong -> ScopeContractError.InputError.InvalidDescription(
            descriptionText = "",
            validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
                maximumLength = domainError.maxLength,
                actualLength = 0,
            ),
        )
    }

    private fun mapAliasError(domainError: ScopeInputError.AliasError): ScopeContractError.InputError.InvalidAlias = when (domainError) {
        is ScopeInputError.AliasError.EmptyAlias -> ScopeContractError.InputError.InvalidAlias(
            alias = "",
            validationFailure = ScopeContractError.AliasValidationFailure.Empty,
        )
        is ScopeInputError.AliasError.AliasTooShort -> ScopeContractError.InputError.InvalidAlias(
            alias = "",
            validationFailure = ScopeContractError.AliasValidationFailure.TooShort(
                minimumLength = domainError.minLength,
                actualLength = 0,
            ),
        )
        is ScopeInputError.AliasError.AliasTooLong -> ScopeContractError.InputError.InvalidAlias(
            alias = "",
            validationFailure = ScopeContractError.AliasValidationFailure.TooLong(
                maximumLength = domainError.maxLength,
                actualLength = 0,
            ),
        )
        is ScopeInputError.AliasError.InvalidAliasFormat -> ScopeContractError.InputError.InvalidAlias(
            alias = domainError.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.InvalidFormat(
                expectedPattern = errorPresenter.presentAliasPattern(domainError.expectedPattern),
            ),
        )
    }

    private fun mapScopeError(domainError: ScopeError): ScopeContractError = when (domainError) {
        is ScopeError.NotFound -> ScopeContractError.BusinessError.NotFound(scopeId = domainError.scopeId.value)
        is ScopeError.ParentNotFound -> ScopeContractError.BusinessError.NotFound(scopeId = domainError.parentId.value)
        is ScopeError.AlreadyDeleted -> ScopeContractError.BusinessError.AlreadyDeleted(scopeId = domainError.scopeId.value)
        is ScopeError.AlreadyArchived -> ScopeContractError.BusinessError.ArchivedScope(scopeId = domainError.scopeId.value)
        is ScopeError.DuplicateTitle -> ScopeContractError.BusinessError.DuplicateTitle(
            title = domainError.title,
            parentId = domainError.parentId?.value,
        )
        is ScopeError.NotArchived -> ScopeContractError.BusinessError.NotArchived(scopeId = domainError.scopeId.value)
        is ScopeError.HasChildren -> ScopeContractError.BusinessError.HasChildren(
            scopeId = domainError.scopeId.value,
            childrenCount = domainError.childCount,
        )
        is ScopeError.VersionMismatch -> ScopeContractError.SystemError.ConcurrentModification(
            scopeId = domainError.scopeId.value,
            expectedVersion = domainError.expectedVersion,
            actualVersion = domainError.actualVersion,
        )
        // Alias-related errors
        is ScopeError.DuplicateAlias -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = domainError.aliasName,
        )
        is ScopeError.AliasNotFound -> ScopeContractError.BusinessError.NotFound(scopeId = domainError.scopeId.value)
        is ScopeError.CannotRemoveCanonicalAlias -> ScopeContractError.BusinessError.CannotRemoveCanonicalAlias(
            scopeId = domainError.scopeId.value,
            aliasName = domainError.aliasId,
        )
        is ScopeError.NoCanonicalAlias -> ScopeContractError.BusinessError.NotFound(scopeId = domainError.scopeId.value)
        // Aspect-related errors
        is ScopeError.AspectNotFound -> ScopeContractError.BusinessError.NotFound(scopeId = domainError.scopeId.value)
        // Event-related errors
        is ScopeError.InvalidEventSequence -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "event-sourcing",
        )
        // Invalid state error
        is ScopeError.InvalidState -> ScopeContractError.SystemError.ServiceUnavailable(
            service = SCOPE_MANAGEMENT_SERVICE,
        )
        // Event replay errors
        is ScopeError.EventApplicationFailed -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "event-sourcing",
        )
        is ScopeError.AliasRecordNotFound -> ScopeContractError.DataInconsistency.MissingCanonicalAlias(
            scopeId = domainError.aggregateId,
        )
    }

    private fun mapUniquenessError(domainError: ScopeUniquenessError): ScopeContractError = when (domainError) {
        is ScopeUniquenessError.DuplicateTitleInContext -> ScopeContractError.BusinessError.DuplicateTitle(
            title = domainError.title,
            parentId = domainError.parentId?.value,
            existingScopeId = domainError.existingId.value,
        )
        is ScopeUniquenessError.DuplicateIdentifier -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = domainError.identifier,
        )
    }

    private fun mapHierarchyError(domainError: ScopeHierarchyError): ScopeContractError = when (domainError) {
        is ScopeHierarchyError.CircularDependency -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.CircularReference(
                scopeId = domainError.scopeId.value,
                parentId = domainError.ancestorId.value,
            ),
        )
        is ScopeHierarchyError.MaxDepthExceeded -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.MaxDepthExceeded(
                scopeId = domainError.scopeId.value,
                attemptedDepth = domainError.currentDepth + 1,
                maximumDepth = domainError.maxDepth,
            ),
        )
        is ScopeHierarchyError.MaxChildrenExceeded -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.MaxChildrenExceeded(
                parentId = domainError.parentId.value,
                currentChildrenCount = domainError.currentCount,
                maximumChildren = domainError.maxChildren,
            ),
        )
        is ScopeHierarchyError.HierarchyUnavailable -> mapSystemError()
    }

    private fun mapAliasErrorDomain(domainError: ScopeAliasError): ScopeContractError = when (domainError) {
        is ScopeAliasError.DuplicateAlias -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = domainError.aliasName.value,
            existingScopeId = domainError.existingScopeId.value,
            attemptedScopeId = domainError.attemptedScopeId.value,
        )
        is ScopeAliasError.AliasNotFoundByName -> ScopeContractError.BusinessError.AliasNotFound(
            alias = domainError.alias,
        )
        is ScopeAliasError.CannotRemoveCanonicalAlias -> ScopeContractError.BusinessError.CannotRemoveCanonicalAlias(
            scopeId = domainError.scopeId.value,
            aliasName = domainError.alias,
        )
        // Handle specific DataInconsistencyError subtypes first
        is ScopeAliasError.DataInconsistencyError.AliasReferencesNonExistentScope -> ScopeContractError.BusinessError.NotFound(
            scopeId = domainError.scopeId.toString(),
        )
        is ScopeAliasError.AliasError -> ScopeContractError.InputError.InvalidAlias(
            alias = domainError.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.InvalidFormat(
                expectedPattern = domainError.reason,
            ),
        )
        is ScopeAliasError.AliasNotFoundById -> ScopeContractError.BusinessError.AliasNotFound(
            alias = domainError.aliasId.value,
        )
        // Handle generic DataInconsistencyError and AliasGenerationFailed together
        is ScopeAliasError.AliasGenerationFailed,
        is ScopeAliasError.DataInconsistencyError,
        -> ScopeContractError.SystemError.ServiceUnavailable(
            service = SCOPE_MANAGEMENT_SERVICE,
        )
    }

    private fun mapNotFoundError(domainError: ScopesError.NotFound): ScopeContractError = when (domainError.identifierType) {
        "alias" -> ScopeContractError.BusinessError.AliasNotFound(alias = domainError.identifier)
        else -> ScopeContractError.BusinessError.NotFound(scopeId = domainError.identifier)
    }

    private fun mapAlreadyExistsError(domainError: ScopesError.AlreadyExists): ScopeContractError = when (domainError.identifierType) {
        "alias" -> ScopeContractError.BusinessError.DuplicateAlias(alias = domainError.identifier)
        "title" -> ScopeContractError.BusinessError.DuplicateTitle(
            title = domainError.identifier,
            parentId = null,
            existingScopeId = null,
        )
        else -> ScopeContractError.BusinessError.DuplicateAlias(alias = domainError.identifier)
    }

    private fun mapSystemError(domainError: ScopesError.SystemError): ScopeContractError.SystemError.ServiceUnavailable =
        ScopeContractError.SystemError.ServiceUnavailable(service = domainError.service ?: SCOPE_MANAGEMENT_SERVICE)

    private fun mapValidationError(domainError: ScopesError.ValidationFailed): ScopeContractError.InputError = when (domainError.field) {
        "title" -> ScopeContractError.InputError.InvalidTitle(
            title = domainError.value,
            validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(emptyList()),
        )
        "description" -> ScopeContractError.InputError.InvalidDescription(
            descriptionText = domainError.value,
            validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(1000, domainError.value.length),
        )
        else -> ScopeContractError.InputError.InvalidId(
            id = domainError.value,
            expectedFormat = "Valid ${domainError.field} value",
        )
    }

    private fun mapInvalidOperationError(domainError: ScopesError.InvalidOperation): ScopeContractError = when (domainError.reason) {
        ScopesError.InvalidOperation.InvalidOperationReason.INVALID_STATE ->
            ScopeContractError.BusinessError.ArchivedScope(scopeId = domainError.entityId ?: "")
        ScopesError.InvalidOperation.InvalidOperationReason.OPERATION_NOT_ALLOWED -> when (domainError.operation) {
            "delete" -> ScopeContractError.BusinessError.HasChildren(
                scopeId = domainError.entityId ?: "",
                childrenCount = null,
            )
            "removeCanonicalAlias" -> ScopeContractError.BusinessError.CannotRemoveCanonicalAlias(
                scopeId = domainError.entityId ?: "",
                aliasName = "",
            )
            else -> ScopeContractError.SystemError.ServiceUnavailable(service = SCOPE_MANAGEMENT_SERVICE)
        }
        else -> ScopeContractError.SystemError.ServiceUnavailable(service = SCOPE_MANAGEMENT_SERVICE)
    }

    private fun mapConflictError(domainError: ScopesError.Conflict): ScopeContractError = when (domainError.conflictType) {
        ScopesError.Conflict.ConflictType.HAS_DEPENDENCIES -> ScopeContractError.BusinessError.HasChildren(
            scopeId = domainError.resourceId,
            childrenCount = null,
        )
        else -> ScopeContractError.BusinessError.DuplicateAlias(alias = domainError.resourceId)
    }
}
