package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.error.BaseErrorMapper
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError
import io.github.kamiazya.scopes.scopemanagement.domain.error.*
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError as AppScopeInputError

/**
 * Maps domain and application errors to contract layer errors.
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
    /**
     * Maps a ScopesError to a ScopeContractError.
     *
     * @param domainError The domain/application error to map
     * @return The corresponding contract error
     */
    override fun mapToContractError(domainError: ScopesError): ScopeContractError = when (domainError) {
        // Input validation errors
        is ScopeInputError.IdError.Blank -> ScopeContractError.InputError.InvalidId(
            id = error.attemptedValue,
            expectedFormat = "Non-empty ULID format",
        )
        is ScopeInputError.IdError.InvalidFormat -> ScopeContractError.InputError.InvalidId(
            id = error.attemptedValue,
            expectedFormat = error.expectedFormat,
        )
        is ScopeInputError.TitleError.Empty -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.Empty,
        )
        is ScopeInputError.TitleError.TooShort -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                minimumLength = error.minimumLength,
                actualLength = error.attemptedValue.length,
            ),
        )
        is ScopeInputError.TitleError.TooLong -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.attemptedValue.length,
            ),
        )
        is ScopeInputError.TitleError.ContainsProhibitedCharacters -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(
                prohibitedCharacters = error.prohibitedCharacters,
            ),
        )
        is ScopeInputError.DescriptionError.TooLong -> ScopeContractError.InputError.InvalidDescription(
            description = error.attemptedValue,
            validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.attemptedValue.length,
            ),
        )

        // Alias validation errors (map to title for now, as aliases are like alternative titles)
        is ScopeInputError.AliasError.Empty -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.Empty,
        )
        is ScopeInputError.AliasError.TooShort -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                minimumLength = error.minimumLength,
                actualLength = error.attemptedValue.length,
            ),
        )
        is ScopeInputError.AliasError.TooLong -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.attemptedValue.length,
            ),
        )
        is ScopeInputError.AliasError.InvalidFormat -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(
                prohibitedCharacters = emptyList(), // Pattern validation mapped to character validation
            ),
        )

        // Business rule violations
        is ScopeError.NotFound -> ScopeContractError.BusinessError.NotFound(
            scopeId = error.scopeId.value,
        )
        is ScopeError.ParentNotFound -> ScopeContractError.BusinessError.NotFound(
            scopeId = error.parentId.value,
        )
        is ScopeError.AlreadyDeleted -> ScopeContractError.BusinessError.AlreadyDeleted(
            scopeId = error.scopeId.value,
        )
        is ScopeError.AlreadyArchived -> ScopeContractError.BusinessError.ArchivedScope(
            scopeId = error.scopeId.value,
        )

        // Additional scope errors
        is ScopeError.DuplicateTitle -> ScopeContractError.BusinessError.DuplicateTitle(
            title = error.title,
            parentId = error.parentId?.value,
        )
        is ScopeError.NotArchived -> ScopeContractError.BusinessError.NotArchived(
            scopeId = error.scopeId.value,
        )
        is ScopeError.VersionMismatch -> ScopeContractError.SystemError.ConcurrentModification(
            scopeId = error.scopeId.value,
            expectedVersion = error.expectedVersion,
            actualVersion = error.actualVersion,
        )

        // Uniqueness errors
        is ScopeUniquenessError.DuplicateTitle -> ScopeContractError.BusinessError.DuplicateTitle(
            title = error.title,
            parentId = error.parentScopeId?.value,
            existingScopeId = error.existingScopeId?.value,
        )

        // Hierarchy errors
        is ScopeHierarchyError.CircularReference -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.CircularReference(
                scopeId = error.scopeId.value,
                parentId = error.parentId.value,
                cyclePath = null, // Domain error doesn't provide path
            ),
        )
        is ScopeHierarchyError.CircularPath -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.CircularReference(
                scopeId = error.scopeId.value,
                parentId = error.cyclePath.lastOrNull()?.value ?: "",
                cyclePath = error.cyclePath.map { it.value },
            ),
        )
        is ScopeHierarchyError.MaxDepthExceeded -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.MaxDepthExceeded(
                scopeId = error.scopeId.value,
                attemptedDepth = error.attemptedDepth,
                maximumDepth = error.maximumDepth,
            ),
        )
        is ScopeHierarchyError.MaxChildrenExceeded -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.MaxChildrenExceeded(
                parentId = error.parentScopeId.value,
                currentChildrenCount = error.currentChildrenCount,
                maximumChildren = error.maximumChildren,
            ),
        )
        is ScopeHierarchyError.SelfParenting -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.SelfParenting(
                scopeId = error.scopeId.value,
            ),
        )
        is ScopeHierarchyError.InvalidParentId -> ScopeContractError.InputError.InvalidParentId(
            parentId = error.invalidId,
            expectedFormat = "ULID format",
        )
        is ScopeHierarchyError.ParentNotFound -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.ParentNotFound(
                scopeId = error.scopeId.value,
                parentId = error.parentId.value,
            ),
        )
        is ScopeHierarchyError.HasChildren -> ScopeContractError.BusinessError.HasChildren(
            scopeId = error.scopeId.value,
            childrenCount = null, // Domain error doesn't provide count
        )
        is ScopeHierarchyError.ScopeInHierarchyNotFound -> ScopeContractError.BusinessError.NotFound(
            scopeId = error.scopeId.value,
        )
        is ScopeHierarchyError.HierarchyUnavailable -> {
            val serviceDetail = when (error.reason) {
                AvailabilityReason.TEMPORARILY_UNAVAILABLE -> "hierarchy-unavailable"
                AvailabilityReason.CORRUPTED_HIERARCHY -> "hierarchy-corrupted"
                AvailabilityReason.CONCURRENT_MODIFICATION -> "hierarchy-conflict"
            }
            ScopeContractError.SystemError.ServiceUnavailable(
                service = serviceDetail,
            )
        }

        // Persistence errors
        is PersistenceError.NotFound -> {
            // Map based on entity type
            when (error.entityType) {
                "ScopeAlias" -> ScopeContractError.BusinessError.AliasNotFound(
                    alias = error.entityId ?: "",
                )
                else -> ScopeContractError.BusinessError.NotFound(
                    scopeId = error.entityId ?: "",
                )
            }
        }
        is PersistenceError.StorageUnavailable -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "storage",
        )
        is PersistenceError.DataCorruption -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "storage",
        )
        is PersistenceError.ConcurrencyConflict -> ScopeContractError.SystemError.ConcurrentModification(
            scopeId = error.entityId,
            expectedVersion = error.expectedVersion.toLongOrNull() ?: 0,
            actualVersion = error.actualVersion.toLongOrNull() ?: 0,
        )

        // User preferences integration errors
        is UserPreferencesIntegrationError.PreferencesServiceUnavailable -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "user-preferences",
        )

        // Hierarchy policy errors
        is HierarchyPolicyError.InvalidMaxDepth -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.MaxDepthExceeded(
                scopeId = "", // Policy errors don't have a specific scope
                attemptedDepth = error.attemptedValue,
                maximumDepth = error.minimumAllowed,
            ),
        )
        is HierarchyPolicyError.InvalidMaxChildrenPerScope -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.MaxChildrenExceeded(
                parentId = "", // Policy errors don't have a specific parent
                currentChildrenCount = error.attemptedValue,
                maximumChildren = error.minimumAllowed,
            ),
        )

        // Default mapping for any unmapped errors
        else -> handleUnmappedError(
            unmappedError = domainError,
            fallbackError = ScopeContractError.SystemError.ServiceUnavailable(
                service = "scope-management",
            ),
        )
    }

    /**
     * Maps an ApplicationError to a ScopeContractError.
     *
     * @param error The application error to map
     * @return The corresponding contract error
     */
    fun mapToContractError(error: ApplicationError): ScopeContractError = when (error) {
        // Map application-level input errors
        is AppScopeInputError.AliasNotFound -> ScopeContractError.BusinessError.AliasNotFound(
            alias = error.attemptedValue,
        )
        is AppScopeInputError.IdBlank -> ScopeContractError.InputError.InvalidId(
            id = error.attemptedValue,
            expectedFormat = "Non-empty ULID format",
        )
        is AppScopeInputError.IdInvalidFormat -> ScopeContractError.InputError.InvalidId(
            id = error.attemptedValue,
            expectedFormat = error.expectedFormat,
        )
        is AppScopeInputError.TitleEmpty -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.Empty,
        )
        is AppScopeInputError.TitleTooShort -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                minimumLength = error.minimumLength,
                actualLength = error.attemptedValue.length,
            ),
        )
        is AppScopeInputError.TitleTooLong -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.attemptedValue.length,
            ),
        )
        is AppScopeInputError.TitleContainsProhibitedCharacters -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(
                prohibitedCharacters = error.prohibitedCharacters,
            ),
        )
        is AppScopeInputError.DescriptionTooLong -> ScopeContractError.InputError.InvalidDescription(
            description = error.attemptedValue,
            validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.attemptedValue.length,
            ),
        )
        is AppScopeInputError.AliasEmpty -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.Empty,
        )
        is AppScopeInputError.AliasTooShort -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                minimumLength = error.minimumLength,
                actualLength = error.attemptedValue.length,
            ),
        )
        is AppScopeInputError.AliasTooLong -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.attemptedValue.length,
            ),
        )
        is AppScopeInputError.AliasInvalidFormat -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(
                prohibitedCharacters = emptyList(),
            ),
        )
        is AppScopeInputError.AliasDuplicate -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = error.attemptedValue,
        )
        is AppScopeInputError.CannotRemoveCanonicalAlias ->
            ScopeContractError.BusinessError.CannotRemoveCanonicalAlias(
                alias = error.attemptedValue,
            )
        is AppScopeInputError.InvalidAlias -> ScopeContractError.BusinessError.AliasNotFound(
            alias = error.attemptedValue,
        )
        is AppScopeInputError.InvalidParentId -> ScopeContractError.InputError.InvalidParentId(
            parentId = error.attemptedValue,
            expectedFormat = "ULID format",
        )
        else -> {
            logger.error(
                "Unmapped ApplicationError encountered, mapping to ServiceUnavailable",
                mapOf(
                    "errorClass" to error::class.simpleName.orEmpty(),
                    "errorMessage" to error.toString(),
                    "errorType" to error::class.qualifiedName.orEmpty(),
                ),
            )
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "scope-management",
            )
        }
    }
}
