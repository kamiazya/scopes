package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.error.BaseErrorMapper
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.application.error.CrossAggregateValidationError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeHierarchyApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeUniquenessError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError as AppScopeInputError

/**
 * Maps application errors to contract layer errors.
 *
 * This mapper handles application-layer errors that are not domain errors,
 * ensuring consistent error translation to the contract layer.
 */
class ApplicationErrorMapper(logger: Logger) : BaseErrorMapper<ScopeManagementApplicationError, ScopeContractError>(logger) {
    override fun mapToContractError(domainError: ScopeManagementApplicationError): ScopeContractError = when (domainError) {
        // ID validation errors
        is AppScopeInputError.IdBlank -> ScopeContractError.InputError.InvalidId(
            id = domainError.attemptedValue,
            expectedFormat = "Non-empty ULID format",
        )
        is AppScopeInputError.IdInvalidFormat -> ScopeContractError.InputError.InvalidId(
            id = domainError.attemptedValue,
            expectedFormat = domainError.expectedFormat,
        )

        // Title validation errors
        is AppScopeInputError.TitleEmpty -> ScopeContractError.InputError.InvalidTitle(
            title = domainError.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.Empty,
        )
        is AppScopeInputError.TitleTooShort -> ScopeContractError.InputError.InvalidTitle(
            title = domainError.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                minimumLength = domainError.minimumLength,
                actualLength = domainError.attemptedValue.length,
            ),
        )
        is AppScopeInputError.TitleTooLong -> ScopeContractError.InputError.InvalidTitle(
            title = domainError.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                maximumLength = domainError.maximumLength,
                actualLength = domainError.attemptedValue.length,
            ),
        )
        is AppScopeInputError.TitleContainsProhibitedCharacters -> ScopeContractError.InputError.InvalidTitle(
            title = domainError.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(
                prohibitedCharacters = domainError.prohibitedCharacters,
            ),
        )

        // Description validation errors
        is AppScopeInputError.DescriptionTooLong -> ScopeContractError.InputError.InvalidDescription(
            descriptionText = domainError.attemptedValue,
            validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
                maximumLength = domainError.maximumLength,
                actualLength = domainError.attemptedValue.length,
            ),
        )

        // Alias validation errors
        is AppScopeInputError.AliasEmpty -> ScopeContractError.InputError.InvalidAlias(
            alias = domainError.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.Empty,
        )
        is AppScopeInputError.AliasTooShort -> ScopeContractError.InputError.InvalidAlias(
            alias = domainError.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.TooShort(
                minimumLength = domainError.minimumLength,
                actualLength = domainError.alias.length,
            ),
        )
        is AppScopeInputError.AliasTooLong -> ScopeContractError.InputError.InvalidAlias(
            alias = domainError.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.TooLong(
                maximumLength = domainError.maximumLength,
                actualLength = domainError.alias.length,
            ),
        )
        is AppScopeInputError.AliasInvalidFormat -> ScopeContractError.InputError.InvalidAlias(
            alias = domainError.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.InvalidFormat(
                expectedPattern = domainError.expectedPattern,
            ),
        )

        // Alias business errors
        is AppScopeInputError.AliasNotFound -> ScopeContractError.BusinessError.AliasNotFound(
            alias = domainError.alias,
        )
        is AppScopeInputError.AliasDuplicate -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = domainError.alias,
        )
        is AppScopeInputError.CannotRemoveCanonicalAlias -> ScopeContractError.BusinessError.CannotRemoveCanonicalAlias
        is AppScopeInputError.AliasOfDifferentScope -> ScopeContractError.BusinessError.AliasOfDifferentScope(
            alias = domainError.alias,
            expectedScopeId = domainError.expectedScopeId,
            actualScopeId = domainError.actualScopeId,
        )

        // Other input errors
        is AppScopeInputError.InvalidAlias -> ScopeContractError.BusinessError.AliasNotFound(
            alias = domainError.alias,
        )
        is AppScopeInputError.InvalidParentId -> ScopeContractError.InputError.InvalidParentId(
            parentId = domainError.parentId,
            expectedFormat = "Valid ULID format",
        )

        // Context errors
        is ContextError.ContextInUse -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "scope-management",
        )
        is ContextError.ContextNotFound -> ScopeContractError.BusinessError.NotFound(
            scopeId = domainError.key,
        )
        is ContextError.ContextUpdateConflict -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "scope-management",
        )
        is ContextError.DuplicateContextKey -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = domainError.key,
        )
        is ContextError.InvalidContextSwitch -> ScopeContractError.BusinessError.NotFound(
            scopeId = domainError.key,
        )
        is ContextError.InvalidFilter -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "scope-management",
        )
        is ContextError.KeyInvalidFormat -> ScopeContractError.InputError.InvalidId(
            id = domainError.attemptedKey,
            expectedFormat = "Valid context key format",
        )
        is ContextError.StateNotFound -> ScopeContractError.BusinessError.NotFound(
            scopeId = domainError.contextId,
        )

        // ScopeAliasError
        is ScopeAliasError.AliasDuplicate -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = domainError.aliasName,
        )
        is ScopeAliasError.AliasGenerationFailed -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "scope-management",
        )
        is ScopeAliasError.AliasGenerationValidationFailed -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "scope-management",
        )
        is ScopeAliasError.AliasNotFound -> ScopeContractError.BusinessError.AliasNotFound(
            alias = domainError.aliasName,
        )
        is ScopeAliasError.CannotRemoveCanonicalAlias -> ScopeContractError.BusinessError.CannotRemoveCanonicalAlias
        is ScopeAliasError.DataInconsistencyError.AliasExistsButScopeNotFound -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "scope-management",
        )

        // ScopeHierarchyApplicationError
        is ScopeHierarchyApplicationError.CircularReference -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.CircularReference(
                scopeId = domainError.scopeId,
                parentId = domainError.cyclePath.firstOrNull() ?: "",
                cyclePath = domainError.cyclePath,
            ),
        )
        is ScopeHierarchyApplicationError.HasChildren -> ScopeContractError.BusinessError.HasChildren(
            scopeId = domainError.scopeId,
            childrenCount = domainError.childCount,
        )
        is ScopeHierarchyApplicationError.InvalidParentId -> ScopeContractError.InputError.InvalidParentId(
            parentId = domainError.invalidId,
            expectedFormat = "Valid ULID format",
        )
        is ScopeHierarchyApplicationError.MaxChildrenExceeded -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.MaxChildrenExceeded(
                parentId = domainError.parentScopeId,
                currentChildrenCount = domainError.currentChildrenCount,
                maximumChildren = domainError.maximumChildren,
            ),
        )
        is ScopeHierarchyApplicationError.MaxDepthExceeded -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.MaxDepthExceeded(
                scopeId = domainError.scopeId,
                attemptedDepth = domainError.attemptedDepth,
                maximumDepth = domainError.maximumDepth,
            ),
        )
        is ScopeHierarchyApplicationError.ParentNotFound -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.ParentNotFound(
                scopeId = domainError.scopeId,
                parentId = domainError.parentId,
            ),
        )
        is ScopeHierarchyApplicationError.SelfParenting -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.SelfParenting(
                scopeId = domainError.scopeId,
            ),
        )

        // ScopeUniquenessError
        is ScopeUniquenessError.DuplicateTitle -> ScopeContractError.BusinessError.DuplicateTitle(
            title = domainError.title,
            parentId = domainError.parentScopeId,
            existingScopeId = domainError.existingScopeId,
        )

        // CrossAggregateValidationError
        is CrossAggregateValidationError.CompensationFailure -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "scope-management",
        )
        is CrossAggregateValidationError.CrossReferenceViolation -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "scope-management",
        )
        is CrossAggregateValidationError.EventualConsistencyViolation -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "scope-management",
        )
        is CrossAggregateValidationError.InvariantViolation -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "scope-management",
        )

        // PersistenceError
        is ScopeManagementApplicationError.PersistenceError.ConcurrencyConflict -> {
            val expectedVersion = domainError.expectedVersion.toLongOrNull() ?: run {
                logger.warn(
                    "Failed to parse expected version to Long, using sentinel value",
                    mapOf(
                        "entityId" to domainError.entityId,
                        "expectedVersion" to domainError.expectedVersion,
                    ),
                )
                -1L
            }
            val actualVersion = domainError.actualVersion.toLongOrNull() ?: run {
                logger.warn(
                    "Failed to parse actual version to Long, using sentinel value",
                    mapOf(
                        "entityId" to domainError.entityId,
                        "actualVersion" to domainError.actualVersion,
                    ),
                )
                -1L
            }
            ScopeContractError.SystemError.ConcurrentModification(
                scopeId = domainError.entityId,
                expectedVersion = expectedVersion,
                actualVersion = actualVersion,
            )
        }
        is ScopeManagementApplicationError.PersistenceError.DataCorruption -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "scope-management",
        )
        is ScopeManagementApplicationError.PersistenceError.NotFound -> ScopeContractError.BusinessError.NotFound(
            scopeId = domainError.entityId ?: "",
        )
        is ScopeManagementApplicationError.PersistenceError.StorageUnavailable -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "scope-management",
        )
    }
}
