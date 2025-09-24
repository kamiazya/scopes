package io.github.kamiazya.scopes.scopemanagement.application.error

import io.github.kamiazya.scopes.scopemanagement.application.util.InputSanitizer
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
 * Extension functions for mapping domain layer errors to application layer errors.
 *
 * This file provides a centralized location for error translation between layers,
 * following Clean Architecture principles. Domain errors are transformed into
 * application-specific error types that contain appropriate context for the
 * application layer while hiding domain implementation details.
 *
 * Key principles:
 * - Domain errors are mapped to semantically equivalent application errors
 * - Error context is preserved or enhanced during translation
 * - Input values are sanitized to prevent sensitive data exposure
 * - Fail-fast approach for unmapped error types to catch issues early
 *
 * Usage:
 * ```kotlin
 * domainError.toApplicationError() // For errors with sufficient context
 * domainError.toApplicationError(attemptedValue) // When additional context is needed
 * ```
 */

/**
 * Maps domain persistence errors to application persistence errors.
 * Preserves concurrency conflict details for proper handling at higher layers.
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
 * Maps domain context errors to application context errors.
 *
 * Context errors relate to context view management (filters, keys, names).
 * This mapping preserves validation constraints while presenting user-friendly
 * error messages through the error presenter.
 *
 * @receiver The domain context error to map
 * @return The corresponding application error with appropriate context
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
 * Maps domain scope input errors to application scope input errors.
 *
 * Scope input errors relate to validation of user-provided data (IDs, titles, descriptions, aliases).
 * The attemptedValue parameter is required because domain errors don't carry the original input
 * for security reasons - this must be provided by the calling context.
 *
 * @receiver The domain scope input error to map
 * @param attemptedValue The original input value that caused the error (will be sanitized)
 * @return The corresponding application error with sanitized input preview
 */
fun DomainScopeInputError.toApplicationError(attemptedValue: String): ScopeManagementApplicationError = when (this) {
    is DomainScopeInputError.IdError.EmptyId ->
        AppScopeInputError.IdBlank(preview = InputSanitizer.createPreview(attemptedValue))

    is DomainScopeInputError.IdError.InvalidIdFormat ->
        AppScopeInputError.IdInvalidFormat(
            preview = InputSanitizer.createPreview(attemptedValue),
            expectedFormat = scopeInputErrorPresenter.presentIdFormat(this.expectedFormat),
        )

    is DomainScopeInputError.TitleError.EmptyTitle ->
        AppScopeInputError.TitleEmpty(preview = InputSanitizer.createPreview(attemptedValue))

    is DomainScopeInputError.TitleError.TitleTooShort ->
        AppScopeInputError.TitleTooShort(
            preview = InputSanitizer.createPreview(attemptedValue),
            minimumLength = this.minLength,
        )

    is DomainScopeInputError.TitleError.TitleTooLong ->
        AppScopeInputError.TitleTooLong(
            preview = InputSanitizer.createPreview(attemptedValue),
            maximumLength = this.maxLength,
        )

    is DomainScopeInputError.TitleError.InvalidTitleFormat ->
        AppScopeInputError.TitleContainsProhibitedCharacters(
            preview = InputSanitizer.createPreview(attemptedValue),
            prohibitedCharacters = listOf('<', '>', '&', '"'),
        )

    is DomainScopeInputError.DescriptionError.DescriptionTooLong ->
        AppScopeInputError.DescriptionTooLong(
            preview = InputSanitizer.createPreview(attemptedValue),
            maximumLength = this.maxLength,
        )

    is DomainScopeInputError.AliasError.EmptyAlias ->
        AppScopeInputError.AliasEmpty(preview = InputSanitizer.createPreview(attemptedValue))

    is DomainScopeInputError.AliasError.AliasTooShort ->
        AppScopeInputError.AliasTooShort(
            preview = InputSanitizer.createPreview(attemptedValue),
            minimumLength = this.minLength,
        )

    is DomainScopeInputError.AliasError.AliasTooLong ->
        AppScopeInputError.AliasTooLong(
            preview = InputSanitizer.createPreview(attemptedValue),
            maximumLength = this.maxLength,
        )

    is DomainScopeInputError.AliasError.InvalidAliasFormat ->
        AppScopeInputError.AliasInvalidFormat(
            preview = InputSanitizer.createPreview(attemptedValue),
            expectedPattern = scopeInputErrorPresenter.presentAliasPattern(this.expectedPattern),
        )
}

/**
 * Maps domain scope alias errors to application scope alias errors.
 *
 * Alias errors relate to scope alias management (duplicates, not found, canonical alias rules).
 * This mapping preserves important context like scope IDs and alias names while converting
 * domain value objects to primitive types suitable for the application layer.
 *
 * @receiver The domain scope alias error to map
 * @return The corresponding application error with extracted primitive values
 */
fun DomainScopeAliasError.toApplicationError(): ScopeManagementApplicationError = when (this) {
    is DomainScopeAliasError.DuplicateAlias ->
        AppScopeAliasError.AliasDuplicate(
            aliasName = this.aliasName.value,
            existingScopeId = this.existingScopeId.value,
            attemptedScopeId = this.attemptedScopeId.value,
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
            alias = this.alias,
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
 * Generic fallback mapper for any domain error that doesn't have a specific mapping.
 *
 * This function provides a last-resort mapping for domain errors to application errors.
 * It should be used sparingly - prefer context-specific mappings in handlers that can
 * provide better error context and more appropriate error types.
 *
 * The mapping strategy:
 * - Known error types are delegated to their specific mappers
 * - Common patterns (NotFound, ValidationFailed) are handled generically
 * - Unknown errors fall back to StorageUnavailable for safety
 *
 * @receiver Any domain error that extends ScopesError
 * @return A generic application error that preserves as much context as possible
 */
fun ScopesError.toGenericApplicationError(): ScopeManagementApplicationError = when (this) {
    is DomainPersistenceError -> this.toApplicationError()
    is ContextError -> this.toApplicationError()
    is DomainScopeInputError -> this.toApplicationError("") // Empty string as fallback when attemptedValue is not available
    is DomainScopeAliasError -> this.toApplicationError()

    // Map hierarchy errors to appropriate application errors
    is ScopeHierarchyError.HierarchyUnavailable -> {
        AppPersistenceError.StorageUnavailable(
            operation = "hierarchy.${this.operation.name.lowercase()}",
        )
    }

    // Map other hierarchy errors to generic persistence errors
    is ScopeHierarchyError -> AppPersistenceError.StorageUnavailable(
        operation = "hierarchy",
    )

    // Map common domain errors to application errors
    is ScopesError.SystemError -> AppPersistenceError.StorageUnavailable(
        operation = this.context["operation"]?.toString() ?: "system-operation",
    )

    is ScopesError.NotFound -> AppPersistenceError.NotFound(
        entityType = this.entityType,
        entityId = this.identifier,
    )

    is ScopesError.ValidationFailed -> AppScopeInputError.ValidationFailed(
        field = this.field,
        preview = InputSanitizer.createPreview(this.value),
        reason = "Validation failed: ${this.constraint}",
    )

    // For other errors, create a generic persistence error
    // This should be replaced with context-specific errors in actual handlers
    else -> AppPersistenceError.StorageUnavailable(
        operation = "domain-operation",
    )
}
