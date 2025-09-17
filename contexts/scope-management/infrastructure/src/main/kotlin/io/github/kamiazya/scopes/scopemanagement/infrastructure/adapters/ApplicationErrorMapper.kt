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

    companion object {
        private const val SERVICE_NAME = "scope-management"
    }

    private fun parseVersionToLong(entityId: String, version: String, versionType: String): Long = version.toLongOrNull() ?: run {
        logger.warn(
            "Failed to parse $versionType version to Long, using sentinel value",
            mapOf(
                "entityId" to entityId,
                "${versionType}Version" to version,
            ),
        )
        -1L
    }

    private fun mapSystemError(): ScopeContractError = ScopeContractError.SystemError.ServiceUnavailable(service = SERVICE_NAME)

    private fun mapAliasNotFoundError(alias: String): ScopeContractError = ScopeContractError.BusinessError.AliasNotFound(alias = alias)

    private fun mapNotFoundError(scopeId: String): ScopeContractError = ScopeContractError.BusinessError.NotFound(scopeId = scopeId)

    override fun mapToContractError(domainError: ScopeManagementApplicationError): ScopeContractError = when (domainError) {
        // Group errors by type
        is AppScopeInputError -> mapInputError(domainError)
        is ContextError -> mapContextError(domainError)
        is ScopeAliasError -> mapScopeAliasError(domainError)
        is ScopeHierarchyApplicationError -> mapHierarchyError(domainError)
        is ScopeUniquenessError -> mapUniquenessError(domainError)
        is CrossAggregateValidationError -> mapSystemError()
        is ScopeManagementApplicationError.PersistenceError -> mapPersistenceError(domainError)
    }

    private fun mapInputError(error: AppScopeInputError): ScopeContractError = when (error) {
        // ID validation errors
        is AppScopeInputError.IdBlank -> ScopeContractError.InputError.InvalidId(
            id = error.attemptedValue,
            expectedFormat = "Non-empty ULID format",
        )
        is AppScopeInputError.IdInvalidFormat -> ScopeContractError.InputError.InvalidId(
            id = error.attemptedValue,
            expectedFormat = error.expectedFormat,
        )

        // Title validation errors
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

        // Description validation errors
        is AppScopeInputError.DescriptionTooLong -> ScopeContractError.InputError.InvalidDescription(
            descriptionText = error.attemptedValue,
            validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.attemptedValue.length,
            ),
        )

        // Alias validation errors
        is AppScopeInputError.AliasEmpty -> ScopeContractError.InputError.InvalidAlias(
            alias = error.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.Empty,
        )
        is AppScopeInputError.AliasTooShort -> ScopeContractError.InputError.InvalidAlias(
            alias = error.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.TooShort(
                minimumLength = error.minimumLength,
                actualLength = error.alias.length,
            ),
        )
        is AppScopeInputError.AliasTooLong -> ScopeContractError.InputError.InvalidAlias(
            alias = error.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.alias.length,
            ),
        )
        is AppScopeInputError.AliasInvalidFormat -> ScopeContractError.InputError.InvalidAlias(
            alias = error.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.InvalidFormat(
                expectedPattern = error.expectedPattern,
            ),
        )

        // Alias business errors
        is AppScopeInputError.AliasNotFound -> mapAliasNotFoundError(error.alias)
        is AppScopeInputError.AliasDuplicate -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = error.alias,
        )
        is AppScopeInputError.CannotRemoveCanonicalAlias -> ScopeContractError.BusinessError.CannotRemoveCanonicalAlias
        is AppScopeInputError.AliasOfDifferentScope -> ScopeContractError.BusinessError.AliasOfDifferentScope(
            alias = error.alias,
            expectedScopeId = error.expectedScopeId,
            actualScopeId = error.actualScopeId,
        )

        // Other input errors
        is AppScopeInputError.InvalidAlias -> mapAliasNotFoundError(error.alias)
        is AppScopeInputError.InvalidParentId -> ScopeContractError.InputError.InvalidParentId(
            parentId = error.parentId,
            expectedFormat = "Valid ULID format",
        )
    }

    private fun mapContextError(error: ContextError): ScopeContractError = when (error) {
        // System errors
        is ContextError.ContextInUse,
        is ContextError.ContextUpdateConflict,
        is ContextError.InvalidFilter,
        -> mapSystemError()

        // Not found errors
        is ContextError.ContextNotFound -> mapNotFoundError(error.key)
        is ContextError.InvalidContextSwitch -> mapNotFoundError(error.key)
        is ContextError.StateNotFound -> mapNotFoundError(error.contextId)

        // Other errors
        is ContextError.DuplicateContextKey -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = error.key,
        )
        is ContextError.KeyInvalidFormat -> ScopeContractError.InputError.InvalidId(
            id = error.attemptedKey,
            expectedFormat = "Valid context key format",
        )
    }

    private fun mapScopeAliasError(error: ScopeAliasError): ScopeContractError = when (error) {
        // System errors
        is ScopeAliasError.AliasGenerationFailed,
        is ScopeAliasError.AliasGenerationValidationFailed,
        is ScopeAliasError.DataInconsistencyError.AliasExistsButScopeNotFound,
        -> mapSystemError()

        // Business errors
        is ScopeAliasError.AliasDuplicate -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = error.aliasName,
        )
        is ScopeAliasError.AliasNotFound -> mapAliasNotFoundError(error.aliasName)
        is ScopeAliasError.CannotRemoveCanonicalAlias -> ScopeContractError.BusinessError.CannotRemoveCanonicalAlias
    }

    private fun mapHierarchyError(error: ScopeHierarchyApplicationError): ScopeContractError = when (error) {
        is ScopeHierarchyApplicationError.CircularReference -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.CircularReference(
                scopeId = error.scopeId,
                parentId = error.cyclePath.firstOrNull() ?: "",
                cyclePath = error.cyclePath,
            ),
        )
        is ScopeHierarchyApplicationError.HasChildren -> ScopeContractError.BusinessError.HasChildren(
            scopeId = error.scopeId,
            childrenCount = error.childCount,
        )
        is ScopeHierarchyApplicationError.InvalidParentId -> ScopeContractError.InputError.InvalidParentId(
            parentId = error.invalidId,
            expectedFormat = "Valid ULID format",
        )
        is ScopeHierarchyApplicationError.MaxChildrenExceeded -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.MaxChildrenExceeded(
                parentId = error.parentScopeId,
                currentChildrenCount = error.currentChildrenCount,
                maximumChildren = error.maximumChildren,
            ),
        )
        is ScopeHierarchyApplicationError.MaxDepthExceeded -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.MaxDepthExceeded(
                scopeId = error.scopeId,
                attemptedDepth = error.attemptedDepth,
                maximumDepth = error.maximumDepth,
            ),
        )
        is ScopeHierarchyApplicationError.ParentNotFound -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.ParentNotFound(
                scopeId = error.scopeId,
                parentId = error.parentId,
            ),
        )
        is ScopeHierarchyApplicationError.SelfParenting -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.SelfParenting(
                scopeId = error.scopeId,
            ),
        )
    }

    private fun mapUniquenessError(error: ScopeUniquenessError): ScopeContractError = when (error) {
        is ScopeUniquenessError.DuplicateTitle -> ScopeContractError.BusinessError.DuplicateTitle(
            title = error.title,
            parentId = error.parentScopeId,
            existingScopeId = error.existingScopeId,
        )
    }

    private fun mapPersistenceError(error: ScopeManagementApplicationError.PersistenceError): ScopeContractError = when (error) {
        is ScopeManagementApplicationError.PersistenceError.ConcurrencyConflict -> {
            val expectedVersion = parseVersionToLong(error.entityId, error.expectedVersion, "expected")
            val actualVersion = parseVersionToLong(error.entityId, error.actualVersion, "actual")
            ScopeContractError.SystemError.ConcurrentModification(
                scopeId = error.entityId,
                expectedVersion = expectedVersion,
                actualVersion = actualVersion,
            )
        }
        is ScopeManagementApplicationError.PersistenceError.DataCorruption,
        is ScopeManagementApplicationError.PersistenceError.StorageUnavailable,
        -> mapSystemError()

        is ScopeManagementApplicationError.PersistenceError.NotFound -> mapNotFoundError(error.entityId ?: "")
    }
}
