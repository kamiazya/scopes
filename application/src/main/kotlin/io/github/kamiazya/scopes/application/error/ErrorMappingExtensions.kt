package io.github.kamiazya.scopes.application.error

import io.github.kamiazya.scopes.domain.error.*
import io.github.kamiazya.scopes.domain.valueobject.ContextViewId
import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Extension functions for mapping common domain errors to application errors.
 * These provide reusable mappings for errors that don't require special context.
 */

/**
 * Maps PersistenceError to ApplicationError.PersistenceError
 */
fun PersistenceError.toApplicationError(): ApplicationError = when (this) {
    is PersistenceError.StorageUnavailable ->
        ApplicationError.PersistenceError.StorageUnavailable(
            operation = this.operation,
            cause = this.cause?.message
        )

    is PersistenceError.DataCorruption ->
        ApplicationError.PersistenceError.DataCorruption(
            entityType = this.entityType,
            entityId = this.entityId,
            reason = this.reason
        )

    is PersistenceError.ConcurrencyConflict ->
        ApplicationError.PersistenceError.ConcurrencyConflict(
            entityType = this.entityType,
            entityId = this.entityId,
            expectedVersion = this.expectedVersion.toString(),
            actualVersion = this.actualVersion.toString()
        )
}

/**
 * Maps ContextError.NamingError to ApplicationError.ContextError
 */
fun ContextError.NamingError.toApplicationError(): ApplicationError = when (this) {
    is ContextError.NamingError.Empty ->
        ApplicationError.ContextError.NamingEmpty

    is ContextError.NamingError.AlreadyExists ->
        ApplicationError.ContextError.NamingAlreadyExists(
            attemptedName = this.attemptedName
        )

    is ContextError.NamingError.InvalidFormat ->
        ApplicationError.ContextError.NamingInvalidFormat(
            attemptedName = this.attemptedName
        )
}

/**
 * Maps ContextError.FilterError to ApplicationError.ContextError
 */
fun ContextError.FilterError.toApplicationError(): ApplicationError = when (this) {
    is ContextError.FilterError.InvalidSyntax ->
        ApplicationError.ContextError.FilterInvalidSyntax(
            position = this.position,
            reason = this.reason,
            expression = this.expression
        )

    is ContextError.FilterError.UnknownAspect ->
        ApplicationError.ContextError.FilterUnknownAspect(
            unknownAspectKey = this.unknownAspectKey,
            expression = this.expression
        )

    is ContextError.FilterError.LogicalInconsistency ->
        ApplicationError.ContextError.FilterLogicalInconsistency(
            reason = this.reason,
            expression = this.expression
        )
}

/**
 * Maps ScopeInputError to ApplicationError.ScopeInputError
 */
fun ScopeInputError.toApplicationError(): ApplicationError = when (this) {
    is ScopeInputError.IdError.Blank ->
        ApplicationError.ScopeInputError.IdBlank("")

    is ScopeInputError.IdError.InvalidFormat ->
        ApplicationError.ScopeInputError.IdInvalidFormat(
            attemptedValue = this.attemptedValue,
            expectedFormat = this.expectedFormat
        )

    is ScopeInputError.TitleError.Empty ->
        ApplicationError.ScopeInputError.TitleEmpty("")

    is ScopeInputError.TitleError.TooShort ->
        ApplicationError.ScopeInputError.TitleTooShort(
            attemptedValue = this.attemptedValue,
            minimumLength = this.minimumLength
        )

    is ScopeInputError.TitleError.TooLong ->
        ApplicationError.ScopeInputError.TitleTooLong(
            attemptedValue = this.attemptedValue,
            maximumLength = this.maximumLength
        )

    is ScopeInputError.TitleError.ContainsProhibitedCharacters ->
        ApplicationError.ScopeInputError.TitleContainsProhibitedCharacters(
            attemptedValue = this.attemptedValue,
            prohibitedCharacters = this.prohibitedCharacters
        )

    is ScopeInputError.DescriptionError.TooLong ->
        ApplicationError.ScopeInputError.DescriptionTooLong(
            attemptedValue = this.attemptedValue,
            maximumLength = this.maximumLength
        )

    is ScopeInputError.AliasError.Empty ->
        ApplicationError.ScopeInputError.AliasEmpty("")

    is ScopeInputError.AliasError.TooShort ->
        ApplicationError.ScopeInputError.AliasTooShort(
            attemptedValue = this.attemptedValue,
            minimumLength = this.minimumLength
        )

    is ScopeInputError.AliasError.TooLong ->
        ApplicationError.ScopeInputError.AliasTooLong(
            attemptedValue = this.attemptedValue,
            maximumLength = this.maximumLength
        )

    is ScopeInputError.AliasError.InvalidFormat ->
        ApplicationError.ScopeInputError.AliasInvalidFormat(
            attemptedValue = this.attemptedValue,
            expectedPattern = this.expectedPattern
        )
}

/**
 * Generic fallback for any ScopesError that doesn't have a specific mapping.
 * Use this sparingly - prefer context-specific mappings in handlers.
 */
fun ScopesError.toGenericApplicationError(): ApplicationError = when (this) {
    is PersistenceError -> this.toApplicationError()
    is ContextError.NamingError -> this.toApplicationError()
    is ContextError.FilterError -> this.toApplicationError()
    is ScopeInputError -> this.toApplicationError()

    // For other errors, create a generic persistence error
    // This should be replaced with context-specific errors in actual handlers
    else -> ApplicationError.PersistenceError.StorageUnavailable(
        operation = "unknown",
        cause = "Unmapped domain error: ${this::class.simpleName}"
    )
}
