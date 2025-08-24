package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.scopemanagement.domain.error.*

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
 */
object ErrorMapper {
    /**
     * Maps a ScopesError to a ScopeContractError.
     *
     * @param error The domain/application error to map
     * @return The corresponding contract error
     */
    fun mapToContractError(error: ScopesError): ScopeContractError = when (error) {
        // Input validation errors
        is ScopeInputError.IdError.Blank -> ScopeContractError.InputError.InvalidId(
            id = error.attemptedValue,
        )
        is ScopeInputError.IdError.InvalidFormat -> ScopeContractError.InputError.InvalidId(
            id = error.attemptedValue,
        )
        is ScopeInputError.TitleError.Empty -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            reason = "Title cannot be empty",
        )
        is ScopeInputError.TitleError.TooShort -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            reason = "Title is too short (minimum ${error.minimumLength} characters)",
        )
        is ScopeInputError.TitleError.TooLong -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            reason = "Title is too long (maximum ${error.maximumLength} characters)",
        )
        is ScopeInputError.TitleError.ContainsProhibitedCharacters -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            reason = "Title contains prohibited characters: ${error.prohibitedCharacters.joinToString(", ")}",
        )
        is ScopeInputError.DescriptionError.TooLong -> ScopeContractError.InputError.InvalidDescription(
            description = error.attemptedValue,
            reason = "Description is too long (maximum ${error.maximumLength} characters)",
        )

        // Alias validation errors
        is ScopeInputError.AliasError.Empty -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            reason = "Alias cannot be empty",
        )
        is ScopeInputError.AliasError.TooShort -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            reason = "Alias is too short (minimum ${error.minimumLength} characters)",
        )
        is ScopeInputError.AliasError.TooLong -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            reason = "Alias is too long (maximum ${error.maximumLength} characters)",
        )
        is ScopeInputError.AliasError.InvalidFormat -> ScopeContractError.InputError.InvalidTitle(
            title = error.attemptedValue,
            reason = "Invalid alias format: expected pattern ${error.expectedPattern}",
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
        )

        // Hierarchy errors
        is ScopeHierarchyError.CircularReference -> ScopeContractError.BusinessError.HierarchyViolation(
            reason = "Circular reference detected: scope ${error.scopeId.value} cannot have parent ${error.parentId.value}",
        )
        is ScopeHierarchyError.CircularPath -> ScopeContractError.BusinessError.HierarchyViolation(
            reason = "Circular path detected: ${error.cyclePath.joinToString(" -> ") { it.value }}",
        )
        is ScopeHierarchyError.MaxDepthExceeded -> ScopeContractError.BusinessError.HierarchyViolation(
            reason = "Maximum hierarchy depth exceeded: attempted ${error.attemptedDepth}, maximum ${error.maximumDepth}",
        )
        is ScopeHierarchyError.MaxChildrenExceeded -> ScopeContractError.BusinessError.HierarchyViolation(
            reason = "Maximum children exceeded for parent ${error.parentScopeId.value}: " +
                "current ${error.currentChildrenCount}, maximum ${error.maximumChildren}",
        )
        is ScopeHierarchyError.InvalidParentId -> ScopeContractError.InputError.InvalidParentId(
            parentId = error.invalidId,
        )
        is ScopeHierarchyError.ParentNotFound -> ScopeContractError.BusinessError.NotFound(
            scopeId = error.parentId.value,
        )

        // Persistence errors
        is PersistenceError.StorageUnavailable -> ScopeContractError.SystemError.ServiceUnavailable(
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

        // Default mapping for any unmapped errors
        else -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "scope-management",
        )
    }
}
