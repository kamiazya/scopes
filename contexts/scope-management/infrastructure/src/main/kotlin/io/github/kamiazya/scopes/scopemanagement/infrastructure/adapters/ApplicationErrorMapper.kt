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

    private fun mapAliasToNotFound(error: AppScopeInputError): ScopeContractError {
        val alias = when (error) {
            is AppScopeInputError.AliasNotFound -> error.alias
            is AppScopeInputError.InvalidAlias -> error.alias
            else -> error("Unexpected error type: $error")
        }
        return mapAliasNotFoundError(alias)
    }

    private fun mapContextToNotFound(error: ContextError): ScopeContractError {
        val key = when (error) {
            is ContextError.ContextNotFound -> error.key
            is ContextError.InvalidContextSwitch -> error.key
            else -> error("Unexpected error type: $error")
        }
        return mapNotFoundError(key)
    }

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
        is AppScopeInputError.IdBlank,
        is AppScopeInputError.IdInvalidFormat,
        -> mapIdInputError(error)

        // Title validation errors
        is AppScopeInputError.TitleEmpty,
        is AppScopeInputError.TitleTooShort,
        is AppScopeInputError.TitleTooLong,
        is AppScopeInputError.TitleContainsProhibitedCharacters,
        -> mapTitleInputError(error)

        // Description validation errors
        is AppScopeInputError.DescriptionTooLong -> mapDescriptionInputError(error)

        // Alias validation errors
        is AppScopeInputError.AliasEmpty,
        is AppScopeInputError.AliasTooShort,
        is AppScopeInputError.AliasTooLong,
        is AppScopeInputError.AliasInvalidFormat,
        -> mapAliasValidationError(error)

        // Alias business errors
        is AppScopeInputError.AliasNotFound,
        is AppScopeInputError.AliasDuplicate,
        is AppScopeInputError.CannotRemoveCanonicalAlias,
        is AppScopeInputError.AliasOfDifferentScope,
        is AppScopeInputError.InvalidAlias,
        -> mapAliasBusinessError(error)

        // Other input errors
        is AppScopeInputError.InvalidParentId -> ScopeContractError.InputError.InvalidParentId(
            parentId = error.parentId,
            expectedFormat = "Valid ULID format",
        )
    }

    private fun mapIdInputError(error: AppScopeInputError): ScopeContractError.InputError.InvalidId = when (error) {
        is AppScopeInputError.IdBlank -> ScopeContractError.InputError.InvalidId(
            id = error.attemptedValue,
            expectedFormat = "Non-empty ULID format",
        )
        is AppScopeInputError.IdInvalidFormat -> ScopeContractError.InputError.InvalidId(
            id = error.attemptedValue,
            expectedFormat = error.expectedFormat,
        )
        else -> error("Unexpected ID error type: $error")
    }

    private fun createInvalidTitle(title: String, validationFailure: ScopeContractError.TitleValidationFailure): ScopeContractError.InputError.InvalidTitle =
        ScopeContractError.InputError.InvalidTitle(title = title, validationFailure = validationFailure)

    private fun mapTitleInputError(error: AppScopeInputError): ScopeContractError.InputError.InvalidTitle = when (error) {
        is AppScopeInputError.TitleEmpty -> createInvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.Empty,
        )
        is AppScopeInputError.TitleTooShort -> createInvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                minimumLength = error.minimumLength,
                actualLength = error.attemptedValue.length,
            ),
        )
        is AppScopeInputError.TitleTooLong -> createInvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.attemptedValue.length,
            ),
        )
        is AppScopeInputError.TitleContainsProhibitedCharacters -> createInvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(
                prohibitedCharacters = error.prohibitedCharacters,
            ),
        )
        else -> error("Unexpected title error type: $error")
    }

    private fun mapDescriptionInputError(error: AppScopeInputError.DescriptionTooLong): ScopeContractError.InputError.InvalidDescription =
        ScopeContractError.InputError.InvalidDescription(
            descriptionText = error.attemptedValue,
            validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.attemptedValue.length,
            ),
        )

    private fun createInvalidAlias(alias: String, validationFailure: ScopeContractError.AliasValidationFailure): ScopeContractError.InputError.InvalidAlias =
        ScopeContractError.InputError.InvalidAlias(alias = alias, validationFailure = validationFailure)

    private fun mapAliasValidationError(error: AppScopeInputError): ScopeContractError.InputError.InvalidAlias = when (error) {
        is AppScopeInputError.AliasEmpty -> createInvalidAlias(
            alias = error.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.Empty,
        )
        is AppScopeInputError.AliasTooShort -> createInvalidAlias(
            alias = error.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.TooShort(
                minimumLength = error.minimumLength,
                actualLength = error.alias.length,
            ),
        )
        is AppScopeInputError.AliasTooLong -> createInvalidAlias(
            alias = error.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.alias.length,
            ),
        )
        is AppScopeInputError.AliasInvalidFormat -> createInvalidAlias(
            alias = error.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.InvalidFormat(
                expectedPattern = error.expectedPattern,
            ),
        )
        else -> error("Unexpected alias validation error type: $error")
    }

    private fun mapAliasBusinessError(error: AppScopeInputError): ScopeContractError = when (error) {
        is AppScopeInputError.AliasNotFound,
        is AppScopeInputError.InvalidAlias,
        -> mapAliasToNotFound(error)
        is AppScopeInputError.AliasDuplicate -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = error.alias,
        )
        is AppScopeInputError.CannotRemoveCanonicalAlias -> ScopeContractError.BusinessError.CannotRemoveCanonicalAlias
        is AppScopeInputError.AliasOfDifferentScope -> ScopeContractError.BusinessError.AliasOfDifferentScope(
            alias = error.alias,
            expectedScopeId = error.expectedScopeId,
            actualScopeId = error.actualScopeId,
        )
        else -> error("Unexpected alias business error type: $error")
    }

    private fun mapContextError(error: ContextError): ScopeContractError = when (error) {
        // System errors
        is ContextError.ContextInUse,
        is ContextError.ContextUpdateConflict,
        is ContextError.InvalidFilter,
        -> mapSystemError()

        // Not found errors
        is ContextError.ContextNotFound,
        is ContextError.InvalidContextSwitch,
        -> mapContextToNotFound(error)
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
