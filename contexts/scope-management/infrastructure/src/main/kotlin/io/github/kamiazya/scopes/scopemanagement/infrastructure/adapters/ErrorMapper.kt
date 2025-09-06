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
    override fun mapToContractError(domainError: ScopesError): ScopeContractError = when (domainError) {
        // Input validation errors
        is ScopeInputError.IdError.Blank -> ScopeContractError.InputError.InvalidId(
            id = domainError.attemptedValue,
            expectedFormat = "Non-empty ULID format",
        )
        is ScopeInputError.IdError.InvalidFormat -> ScopeContractError.InputError.InvalidId(
            id = domainError.attemptedValue,
            expectedFormat = domainError.expectedFormat,
        )
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
        is ScopeInputError.DescriptionError.TooLong -> ScopeContractError.InputError.InvalidDescription(
            descriptionText = domainError.attemptedValue,
            validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
                maximumLength = domainError.maximumLength,
                actualLength = domainError.attemptedValue.length,
            ),
        )

        // Alias validation errors (map to title for now, as aliases are like alternative titles)
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
                prohibitedCharacters = emptyList(), // Pattern validation mapped to character validation
            ),
        )

        // Business rule violations
        is ScopeError.NotFound -> ScopeContractError.BusinessError.NotFound(
            scopeId = domainError.scopeId.value,
        )
        is ScopeError.ParentNotFound -> ScopeContractError.BusinessError.NotFound(
            scopeId = domainError.parentId.value,
        )
        is ScopeError.AlreadyDeleted -> ScopeContractError.BusinessError.AlreadyDeleted(
            scopeId = domainError.scopeId.value,
        )
        is ScopeError.AlreadyArchived -> ScopeContractError.BusinessError.ArchivedScope(
            scopeId = domainError.scopeId.value,
        )

        // Additional scope errors
        is ScopeError.DuplicateTitle -> ScopeContractError.BusinessError.DuplicateTitle(
            title = domainError.title,
            parentId = domainError.parentId?.value,
        )
        is ScopeError.NotArchived -> ScopeContractError.BusinessError.NotArchived(
            scopeId = domainError.scopeId.value,
        )
        is ScopeError.VersionMismatch -> ScopeContractError.SystemError.ConcurrentModification(
            scopeId = domainError.scopeId.value,
            expectedVersion = domainError.expectedVersion,
            actualVersion = domainError.actualVersion,
        )

        // Uniqueness errors
        is ScopeUniquenessError.DuplicateTitle -> ScopeContractError.BusinessError.DuplicateTitle(
            title = domainError.title,
            parentId = domainError.parentScopeId?.value,
            existingScopeId = domainError.existingScopeId?.value,
        )

        // New structured errors
        is ScopesError.NotFound -> when (domainError.identifierType) {
            "alias" -> ScopeContractError.BusinessError.AliasNotFound(alias = domainError.identifier)
            else -> ScopeContractError.BusinessError.NotFound(scopeId = domainError.identifier)
        }
        is ScopesError.AlreadyExists -> when (domainError.entityType) {
            "AspectDefinition" -> ScopeContractError.BusinessError.DuplicateAlias(
                alias = domainError.identifier, // Using DuplicateAlias for aspect keys as a workaround
            )
            else -> ScopeContractError.BusinessError.DuplicateAlias(
                alias = domainError.identifier,
            )
        }
        is ScopesError.SystemError -> when (domainError.errorType) {
            ScopesError.SystemError.SystemErrorType.SERVICE_UNAVAILABLE ->
                ScopeContractError.SystemError.ServiceUnavailable(service = domainError.service ?: "scope-management")
            else -> ScopeContractError.SystemError.ServiceUnavailable(
                service = domainError.service ?: "scope-management",
            )
        }
        is ScopesError.ValidationFailed -> {
            // Map validation errors to appropriate input errors
            when (domainError.field) {
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
        }
        is ScopesError.InvalidOperation -> when {
            domainError.reason == ScopesError.InvalidOperation.InvalidOperationReason.INVALID_STATE ->
                ScopeContractError.BusinessError.ArchivedScope(scopeId = domainError.entityId ?: "")
            else -> ScopeContractError.SystemError.ServiceUnavailable(service = "scope-management")
        }
        is ScopesError.Conflict -> when (domainError.conflictType) {
            ScopesError.Conflict.ConflictType.HAS_DEPENDENCIES ->
                ScopeContractError.BusinessError.HasChildren(
                    scopeId = domainError.resourceId,
                    childrenCount = null,
                )
            else -> ScopeContractError.BusinessError.DuplicateAlias(
                alias = domainError.resourceId,
            )
        }
        is ScopesError.ConcurrencyError -> ScopeContractError.SystemError.ConcurrentModification(
            scopeId = domainError.aggregateId,
            expectedVersion = domainError.expectedVersion?.toLong() ?: 0,
            actualVersion = domainError.actualVersion?.toLong() ?: 0,
        )
        is ScopesError.RepositoryError -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "scope-management",
        )

        // Default fallback for unmapped errors
        else -> handleUnmappedError(
            domainError,
            ScopeContractError.SystemError.ServiceUnavailable(service = "scope-management"),
        )
    }
}
