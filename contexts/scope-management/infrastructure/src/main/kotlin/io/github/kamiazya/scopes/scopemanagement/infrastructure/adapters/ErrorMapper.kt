package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.error.BaseErrorMapper
import io.github.kamiazya.scopes.platform.observability.logging.Logger
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
    private fun presentIdFormat(formatType: ScopeInputError.IdError.InvalidFormat.IdFormatType): String = when (formatType) {
        ScopeInputError.IdError.InvalidFormat.IdFormatType.ULID -> "ULID format"
        ScopeInputError.IdError.InvalidFormat.IdFormatType.UUID -> "UUID format"
        ScopeInputError.IdError.InvalidFormat.IdFormatType.NUMERIC_ID -> "numeric ID format"
        ScopeInputError.IdError.InvalidFormat.IdFormatType.CUSTOM_FORMAT -> "custom format"
    }

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
            expectedVersion = domainError.expectedVersion?.toLong() ?: 0,
            actualVersion = domainError.actualVersion?.toLong() ?: 0,
        )
        is ScopesError.RepositoryError -> ScopeContractError.SystemError.ServiceUnavailable(
            service = SCOPE_MANAGEMENT_SERVICE,
        )

        // Default fallback for unmapped errors
        else -> handleUnmappedError(
            domainError,
            ScopeContractError.SystemError.ServiceUnavailable(service = SCOPE_MANAGEMENT_SERVICE),
        )
    }

    private fun mapIdError(domainError: ScopeInputError.IdError): ScopeContractError.InputError.InvalidId = when (domainError) {
        is ScopeInputError.IdError.Blank -> ScopeContractError.InputError.InvalidId(
            id = domainError.attemptedValue,
            expectedFormat = "Non-empty ULID format",
        )
        is ScopeInputError.IdError.InvalidFormat -> ScopeContractError.InputError.InvalidId(
            id = domainError.attemptedValue,
            expectedFormat = presentIdFormat(domainError.formatType),
        )
    }

    private fun mapTitleError(domainError: ScopeInputError.TitleError): ScopeContractError.InputError.InvalidTitle = when (domainError) {
        is ScopeInputError.TitleError.Empty -> ScopeContractError.InputError.InvalidTitle(
            title = domainError.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.Empty,
        )
        is ScopeInputError.TitleError.TooShort -> ScopeContractError.InputError.InvalidTitle(
            title = domainError.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                minimumLength = domainError.minimumLength,
                actualLength = domainError.attemptedValue.length,
            ),
        )
        is ScopeInputError.TitleError.TooLong -> ScopeContractError.InputError.InvalidTitle(
            title = domainError.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                maximumLength = domainError.maximumLength,
                actualLength = domainError.attemptedValue.length,
            ),
        )
        is ScopeInputError.TitleError.ContainsProhibitedCharacters -> ScopeContractError.InputError.InvalidTitle(
            title = domainError.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(
                prohibitedCharacters = domainError.prohibitedCharacters,
            ),
        )
    }

    private fun mapDescriptionError(domainError: ScopeInputError.DescriptionError): ScopeContractError.InputError.InvalidDescription = when (domainError) {
        is ScopeInputError.DescriptionError.TooLong -> ScopeContractError.InputError.InvalidDescription(
            descriptionText = domainError.attemptedValue,
            validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
                maximumLength = domainError.maximumLength,
                actualLength = domainError.attemptedValue.length,
            ),
        )
    }

    private fun mapAliasError(domainError: ScopeInputError.AliasError): ScopeContractError.InputError.InvalidTitle = when (domainError) {
        is ScopeInputError.AliasError.Empty -> ScopeContractError.InputError.InvalidTitle(
            title = domainError.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.Empty,
        )
        is ScopeInputError.AliasError.TooShort -> ScopeContractError.InputError.InvalidTitle(
            title = domainError.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                minimumLength = domainError.minimumLength,
                actualLength = domainError.attemptedValue.length,
            ),
        )
        is ScopeInputError.AliasError.TooLong -> ScopeContractError.InputError.InvalidTitle(
            title = domainError.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                maximumLength = domainError.maximumLength,
                actualLength = domainError.attemptedValue.length,
            ),
        )
        is ScopeInputError.AliasError.InvalidFormat -> ScopeContractError.InputError.InvalidTitle(
            title = domainError.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(
                prohibitedCharacters = emptyList(),
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
        is ScopeError.VersionMismatch -> ScopeContractError.SystemError.ConcurrentModification(
            scopeId = domainError.scopeId.value,
            expectedVersion = domainError.expectedVersion,
            actualVersion = domainError.actualVersion,
        )
    }

    private fun mapUniquenessError(domainError: ScopeUniquenessError): ScopeContractError = when (domainError) {
        is ScopeUniquenessError.DuplicateTitle -> ScopeContractError.BusinessError.DuplicateTitle(
            title = domainError.title,
            parentId = domainError.parentScopeId?.value,
            existingScopeId = domainError.existingScopeId?.value,
        )
        is ScopeUniquenessError.DuplicateIdentifier -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = domainError.identifier,
        )
    }

    private fun mapHierarchyError(domainError: ScopeHierarchyError): ScopeContractError = when (domainError) {
        is ScopeHierarchyError.HasChildren -> ScopeContractError.BusinessError.HasChildren(
            scopeId = domainError.scopeId.value,
            childrenCount = null,
        )
        is ScopeHierarchyError.CircularReference -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.CircularReference(
                scopeId = domainError.scopeId.value,
                parentId = domainError.parentId.value,
            ),
        )
        is ScopeHierarchyError.SelfParenting -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.SelfParenting(
                scopeId = domainError.scopeId.value,
            ),
        )
        is ScopeHierarchyError.ParentNotFound -> ScopeContractError.BusinessError.NotFound(
            scopeId = domainError.parentId.value,
        )
        is ScopeHierarchyError.CircularPath -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.CircularReference(
                scopeId = domainError.scopeId.value,
                parentId = domainError.cyclePath.firstOrNull()?.value ?: domainError.scopeId.value,
            ),
        )
        is ScopeHierarchyError.InvalidParentId -> ScopeContractError.InputError.InvalidId(
            id = domainError.invalidId,
            expectedFormat = "Valid parent scope ID",
        )
        is ScopeHierarchyError.MaxChildrenExceeded -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.MaxChildrenExceeded(
                parentId = domainError.parentScopeId.value,
                currentChildrenCount = domainError.currentChildrenCount,
                maximumChildren = domainError.maximumChildren,
            ),
        )
        is ScopeHierarchyError.MaxDepthExceeded -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.MaxDepthExceeded(
                scopeId = domainError.scopeId.value,
                attemptedDepth = domainError.attemptedDepth,
                maximumDepth = domainError.maximumDepth,
            ),
        )
        is ScopeHierarchyError.ScopeInHierarchyNotFound -> ScopeContractError.BusinessError.NotFound(
            scopeId = domainError.scopeId.value,
        )
        is ScopeHierarchyError.HierarchyUnavailable -> ScopeContractError.SystemError.ServiceUnavailable(
            service = SCOPE_MANAGEMENT_SERVICE,
        )
    }

    private fun mapAliasErrorDomain(domainError: ScopeAliasError): ScopeContractError = when (domainError) {
        is ScopeAliasError.DuplicateAlias -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = domainError.aliasName,
        )
        is ScopeAliasError.AliasNotFound -> ScopeContractError.BusinessError.AliasNotFound(
            alias = domainError.aliasName,
        )
        is ScopeAliasError.CannotRemoveCanonicalAlias -> ScopeContractError.BusinessError.CannotRemoveCanonicalAlias
        is ScopeAliasError.AliasGenerationFailed -> ScopeContractError.SystemError.ServiceUnavailable(
            service = SCOPE_MANAGEMENT_SERVICE,
        )
        is ScopeAliasError.AliasGenerationValidationFailed -> ScopeContractError.InputError.InvalidTitle(
            title = domainError.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(emptyList()),
        )
        is ScopeAliasError.AliasNotFoundById -> ScopeContractError.BusinessError.AliasNotFound(
            alias = domainError.aliasId.value,
        )
        is ScopeAliasError.DataInconsistencyError.AliasExistsButScopeNotFound -> ScopeContractError.BusinessError.NotFound(
            scopeId = domainError.scopeId.value,
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
            "removeCanonicalAlias" -> ScopeContractError.BusinessError.CannotRemoveCanonicalAlias
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
