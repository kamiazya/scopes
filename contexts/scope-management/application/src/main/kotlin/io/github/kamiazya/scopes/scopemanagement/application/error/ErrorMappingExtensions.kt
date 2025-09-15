package io.github.kamiazya.scopes.scopemanagement.application.error

import io.github.kamiazya.scopes.scopemanagement.domain.error.AvailabilityReason
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.application.error.ContextError as AppContextError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeAliasError as AppScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError as AppScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError.PersistenceError as AppPersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError as DomainPersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError as DomainScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError as DomainScopeInputError

// Create singleton presenter instances
private val contextErrorPresenter = ContextErrorPresenter()
private val scopeInputErrorPresenter = ScopeInputErrorPresenter()

/**
 * Extension functions for mapping common domain errors to application errors.
 * These provide reusable mappings for errors that don't require special context.
 */

/**
 * Maps PersistenceError to ApplicationError.PersistenceError
 */
fun DomainPersistenceError.toApplicationError(): ScopeManagementApplicationError = when (this) {
    is DomainPersistenceError.ConcurrencyConflict ->
        AppPersistenceError.ConcurrencyConflict(
            entityType = this.entityType,
            entityId = this.entityId,
            expectedVersion = this.expectedVersion,
            actualVersion = this.actualVersion,
        )
}

/**
 * Maps ContextError to ApplicationError.ContextError
 */
fun ContextError.toApplicationError(): ScopeManagementApplicationError = when (this) {
    is ContextError.KeyTooShort ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "key too short (minimum: ${this.minimumLength})",
        )

    is ContextError.KeyTooLong ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "key too long (maximum: ${this.maximumLength})",
        )

    is ContextError.InvalidKeyFormat ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = contextErrorPresenter.presentInvalidKeyFormat(this.errorType),
        )

    is ContextError.EmptyKey ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "empty key",
        )

    is ContextError.EmptyName ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "empty name",
        )

    is ContextError.NameTooLong ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "name too long (maximum: ${this.maximumLength})",
        )

    is ContextError.EmptyDescription ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "empty description",
        )

    is ContextError.DescriptionTooShort ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "description too short (minimum: ${this.minimumLength})",
        )

    is ContextError.DescriptionTooLong ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "description too long (maximum: ${this.maximumLength})",
        )

    is ContextError.EmptyFilter ->
        AppContextError.InvalidFilter(
            filter = "",
            reason = "Empty filter",
        )

    is ContextError.FilterTooShort ->
        AppContextError.InvalidFilter(
            filter = "",
            reason = "Filter too short (minimum: ${this.minimumLength})",
        )

    is ContextError.FilterTooLong ->
        AppContextError.InvalidFilter(
            filter = "",
            reason = "Filter too long (maximum: ${this.maximumLength})",
        )

    is ContextError.InvalidFilterSyntax ->
        AppContextError.InvalidFilter(
            filter = this.expression,
            reason = contextErrorPresenter.presentInvalidFilterSyntax(this.errorType),
        )

    is ContextError.InvalidScope ->
        AppContextError.StateNotFound(
            contextId = this.scopeId,
        )

    is ContextError.InvalidHierarchy ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = contextErrorPresenter.presentInvalidHierarchy(this.errorType),
        )

    is ContextError.DuplicateScope ->
        AppContextError.KeyInvalidFormat(
            attemptedKey = "Duplicate scope: ${this.title}",
        )
}

/**
 * Maps ScopeInputError to ApplicationError.ScopeInputError
 */
fun DomainScopeInputError.toApplicationError(): ScopeManagementApplicationError = when (this) {
    is DomainScopeInputError.IdError.EmptyId ->
        AppScopeInputError.IdBlank("empty-id")

    is DomainScopeInputError.IdError.InvalidIdFormat ->
        AppScopeInputError.IdInvalidFormat(
            attemptedValue = this.id,
            expectedFormat = scopeInputErrorPresenter.presentIdFormat(this.expectedFormat),
        )

    is DomainScopeInputError.TitleError.EmptyTitle ->
        AppScopeInputError.TitleEmpty("empty-title")

    is DomainScopeInputError.TitleError.TitleTooShort ->
        AppScopeInputError.TitleTooShort(
            attemptedValue = "too-short",
            minimumLength = this.minLength,
        )

    is DomainScopeInputError.TitleError.TitleTooLong ->
        AppScopeInputError.TitleTooLong(
            attemptedValue = "too-long",
            maximumLength = this.maxLength,
        )

    is DomainScopeInputError.TitleError.InvalidTitleFormat ->
        AppScopeInputError.TitleContainsProhibitedCharacters(
            attemptedValue = this.title,
            prohibitedCharacters = listOf('<', '>', '&', '"'),
        )

    is DomainScopeInputError.DescriptionError.DescriptionTooLong ->
        AppScopeInputError.DescriptionTooLong(
            attemptedValue = "too-long",
            maximumLength = this.maxLength,
        )

    is DomainScopeInputError.AliasError.EmptyAlias ->
        AppScopeInputError.AliasEmpty("empty-alias")

    is DomainScopeInputError.AliasError.AliasTooShort ->
        AppScopeInputError.AliasTooShort(
            attemptedValue = "too-short",
            minimumLength = this.minLength,
        )

    is DomainScopeInputError.AliasError.AliasTooLong ->
        AppScopeInputError.AliasTooLong(
            attemptedValue = "too-long",
            maximumLength = this.maxLength,
        )

    is DomainScopeInputError.AliasError.InvalidAliasFormat ->
        AppScopeInputError.AliasInvalidFormat(
            attemptedValue = this.alias,
            expectedPattern = scopeInputErrorPresenter.presentAliasPattern(this.expectedPattern),
        )
}

/**
 * Maps ScopeAliasError to ApplicationError.ScopeAliasError
 */
fun DomainScopeAliasError.toApplicationError(): ScopeManagementApplicationError = when (this) {
    is DomainScopeAliasError.DuplicateAlias ->
        AppScopeAliasError.AliasDuplicate(
            aliasName = this.alias,
            existingScopeId = this.scopeId.toString(),
            attemptedScopeId = "attempted-scope-id", // Domain error doesn't provide attempted scope ID
        )

    is DomainScopeAliasError.AliasNotFoundByName ->
        AppScopeAliasError.AliasNotFound(
            aliasName = this.alias,
        )

    is DomainScopeAliasError.AliasNotFoundById ->
        AppScopeAliasError.AliasNotFound(
            aliasName = "ID:${this.aliasId.value}",
        )

    is DomainScopeAliasError.CannotRemoveCanonicalAlias ->
        AppScopeAliasError.CannotRemoveCanonicalAlias(
            scopeId = this.scopeId.toString(),
            aliasName = this.alias,
        )

    is DomainScopeAliasError.AliasGenerationFailed ->
        AppScopeAliasError.AliasGenerationFailed(
            scopeId = this.scopeId.toString(),
            retryCount = 0, // Default retry count since not available in new structure
        )

    is DomainScopeAliasError.AliasError ->
        AppScopeAliasError.AliasGenerationValidationFailed(
            scopeId = "scope-id",
            reason = this.reason,
            attemptedValue = this.alias,
        )

    is DomainScopeAliasError.DataInconsistencyError.AliasReferencesNonExistentScope ->
        AppScopeAliasError.DataInconsistencyError.AliasExistsButScopeNotFound(
            aliasName = this.aliasId.value,
            scopeId = this.scopeId.toString(),
        )

    // Fail fast for any unmapped DataInconsistencyError subtypes
    is DomainScopeAliasError.DataInconsistencyError ->
        error(
            "Unmapped DataInconsistencyError subtype: ${this::class.simpleName}. " +
                "Please add proper error mapping for this error type.",
        )
}

/**
 * Generic fallback for any ScopesError that doesn't have a specific mapping.
 * Use this sparingly - prefer context-specific mappings in handlers.
 */
fun ScopesError.toGenericApplicationError(): ScopeManagementApplicationError = when (this) {
    is DomainPersistenceError -> this.toApplicationError()
    is ContextError -> this.toApplicationError()
    is DomainScopeInputError -> this.toApplicationError()
    is DomainScopeAliasError -> this.toApplicationError()

    // Map hierarchy errors to appropriate application errors
    is ScopeHierarchyError.HierarchyUnavailable -> {
        val cause = when (this.reason) {
            AvailabilityReason.TEMPORARILY_UNAVAILABLE -> "Hierarchy service temporarily unavailable"
            AvailabilityReason.CORRUPTED_HIERARCHY -> "Hierarchy data corruption detected"
            AvailabilityReason.CONCURRENT_MODIFICATION -> "Concurrent modification conflict"
        }
        AppPersistenceError.StorageUnavailable(
            operation = "hierarchy.${this.operation.name.lowercase()}",
            errorCause = cause,
        )
    }

    // Map other hierarchy errors to generic persistence errors
    is ScopeHierarchyError -> AppPersistenceError.StorageUnavailable(
        operation = "hierarchy",
        errorCause = "Hierarchy error: ${this::class.simpleName}",
    )

    // For other errors, create a generic persistence error
    // This should be replaced with context-specific errors in actual handlers
    else -> AppPersistenceError.StorageUnavailable(
        operation = "domain-operation",
        errorCause = "Unmapped domain error: ${this::class.simpleName}",
    )
}
