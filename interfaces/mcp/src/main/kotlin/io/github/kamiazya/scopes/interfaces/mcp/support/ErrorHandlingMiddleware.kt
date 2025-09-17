package io.github.kamiazya.scopes.interfaces.mcp.support

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.devicesync.errors.DeviceSynchronizationContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.userpreferences.errors.UserPreferencesContractError
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.domain.error.AspectError
import io.github.kamiazya.scopes.scopemanagement.domain.error.DomainValidationError

/**
 * Common error handling middleware for MCP interface.
 *
 * This middleware provides a centralized error mapping strategy,
 * removing duplicate error handling code from individual handlers.
 */
internal class ErrorHandlingMiddleware(private val logger: Logger) {

    /**
     * Wraps a function call with error handling.
     *
     * @param operation The operation name for logging
     * @param block The function to execute
     * @return Either error response or success result
     */
    suspend fun <T, E, R> handle(operation: String, block: suspend () -> Either<E, T>, errorMapper: (E) -> R): Either<R, T> {
        logger.debug("Executing operation: $operation")

        return try {
            when (val result = block()) {
                is Either.Left -> {
                    logger.warn("Operation $operation failed with error: ${result.value}")
                    Either.Left(errorMapper(result.value))
                }
                is Either.Right -> {
                    logger.debug("Operation $operation completed successfully")
                    Either.Right(result.value)
                }
            }
        } catch (e: Exception) {
            logger.error("Unexpected error in operation $operation", throwable = e)
            throw e // Re-throw unexpected exceptions
        }
    }

    /**
     * Maps common contract errors to user-friendly messages.
     */
    fun mapScopeError(error: ScopeContractError): ErrorResponse = when (error) {
        is ScopeContractError.InputError.InvalidTitle -> {
            val msg = when (val failure = error.validationFailure) {
                is ScopeContractError.TitleValidationFailure.Empty ->
                    "Please provide a title for the scope"
                is ScopeContractError.TitleValidationFailure.TooShort ->
                    "The title is too short. Please use at least ${failure.minimumLength} characters"
                is ScopeContractError.TitleValidationFailure.TooLong ->
                    "The title is too long. Please use fewer than ${failure.maximumLength} characters"
                is ScopeContractError.TitleValidationFailure.InvalidCharacters ->
                    "The title contains invalid characters: ${failure.prohibitedCharacters.joinToString()}"
            }
            ErrorResponse(
                code = "INVALID_TITLE",
                message = "Invalid title: ${error.title}",
                userMessage = msg,
            )
        }
        is ScopeContractError.InputError.InvalidDescription ->
            ErrorResponse(
                code = "INVALID_DESCRIPTION",
                message = "Invalid description",
                userMessage = when (val failure = error.validationFailure) {
                    is ScopeContractError.DescriptionValidationFailure.TooLong ->
                        "The description is too long. Please use fewer than ${failure.maximumLength} characters"
                },
            )
        is ScopeContractError.InputError.InvalidId ->
            ErrorResponse(
                code = "INVALID_ID",
                message = "Invalid ID: ${error.id}",
                userMessage = "The provided ID '${error.id}' is not valid. ${error.expectedFormat ?: ""}",
            )
        is ScopeContractError.InputError.InvalidParentId ->
            ErrorResponse(
                code = "INVALID_PARENT_ID",
                message = "Invalid parent ID: ${error.parentId}",
                userMessage = "The parent ID '${error.parentId}' is not valid",
            )
        is ScopeContractError.BusinessError.NotFound ->
            ErrorResponse(
                code = "SCOPE_NOT_FOUND",
                message = "Scope not found: ${error.scopeId}",
                userMessage = "Could not find the requested scope",
            )
        is ScopeContractError.BusinessError.DuplicateAlias ->
            ErrorResponse(
                code = "ALIAS_EXISTS",
                message = "Alias already exists: ${error.alias}",
                userMessage = "The alias '${error.alias}' is already in use. Please choose a different alias",
            )
        is ScopeContractError.BusinessError.HasChildren ->
            ErrorResponse(
                code = "HAS_CHILDREN",
                message = "Cannot delete scope ${error.scopeId}",
                userMessage = "This scope has ${error.childrenCount ?: "some"} child scopes. Please delete or move them first",
            )
        is ScopeContractError.BusinessError.AlreadyDeleted ->
            ErrorResponse(
                code = "ALREADY_DELETED",
                message = "Scope already deleted: ${error.scopeId}",
                userMessage = "This scope has already been deleted",
            )
        is ScopeContractError.BusinessError.ArchivedScope ->
            ErrorResponse(
                code = "ARCHIVED_SCOPE",
                message = "Scope is archived: ${error.scopeId}",
                userMessage = "This scope is archived and cannot be modified",
            )
        is ScopeContractError.BusinessError.DuplicateTitle ->
            ErrorResponse(
                code = "DUPLICATE_TITLE",
                message = "Duplicate title: ${error.title}",
                userMessage = "A scope with the title '${error.title}' already exists in this location",
            )
        is ScopeContractError.BusinessError.HierarchyViolation ->
            ErrorResponse(
                code = "HIERARCHY_VIOLATION",
                message = "Hierarchy violation",
                userMessage = "The requested operation would violate the scope hierarchy",
            )
        is ScopeContractError.BusinessError.AliasNotFound ->
            ErrorResponse(
                code = "ALIAS_NOT_FOUND",
                message = "Alias not found: ${error.alias}",
                userMessage = "The alias '${error.alias}' does not exist",
            )
        is ScopeContractError.BusinessError.CannotRemoveCanonicalAlias ->
            ErrorResponse(
                code = "CANNOT_REMOVE_CANONICAL",
                message = "Cannot remove canonical alias",
                userMessage = "The canonical alias cannot be removed",
            )
        is ScopeContractError.BusinessError.NotArchived ->
            ErrorResponse(
                code = "NOT_ARCHIVED",
                message = "Scope is not archived: ${error.scopeId}",
                userMessage = "This scope is not archived",
            )
        is ScopeContractError.SystemError ->
            ErrorResponse(
                code = "SYSTEM_ERROR",
                message = "System error: ${error.javaClass.simpleName}",
                userMessage = "An internal error occurred. Please try again later",
            )
        else ->
            ErrorResponse(
                code = "UNKNOWN_ERROR",
                message = "Unknown error: $error",
                userMessage = "An unexpected error occurred",
            )
    }

    /**
     * Maps user preferences errors.
     */
    fun mapUserPreferencesError(error: UserPreferencesContractError): ErrorResponse = when (error) {
        is UserPreferencesContractError.InputError.InvalidPreferenceKey ->
            ErrorResponse(
                code = "INVALID_PREFERENCE_KEY",
                message = "Invalid preference key: ${error.key}",
                userMessage = "The preference key '${error.key}' is not valid",
            )
        is UserPreferencesContractError.DataError.PreferencesCorrupted ->
            ErrorResponse(
                code = "PREFERENCES_CORRUPTED",
                message = "Preferences data is corrupted",
                userMessage = "Your preferences data is corrupted. Please reset your settings.",
            )
        is UserPreferencesContractError.DataError.PreferencesMigrationRequired ->
            ErrorResponse(
                code = "PREFERENCES_MIGRATION_REQUIRED",
                message = "Preferences migration required from ${error.fromVersion} to ${error.toVersion}",
                userMessage = "Your preferences need to be updated to a newer format.",
            )
        else ->
            ErrorResponse(
                code = "PREFERENCE_ERROR",
                message = "Preference error: $error",
                userMessage = "An error occurred with user preferences",
            )
    }

    /**
     * Maps device synchronization errors.
     */
    fun mapDeviceSyncError(error: DeviceSynchronizationContractError): ErrorResponse = when (error) {
        is DeviceSynchronizationContractError.BusinessError.SynchronizationFailed -> {
            when (val failure = error.failure) {
                is DeviceSynchronizationContractError.SynchronizationFailureType.NetworkFailure ->
                    ErrorResponse(
                        code = "SYNC_NETWORK_ERROR",
                        message = "Network error during sync between ${failure.localDeviceId} and ${failure.remoteDeviceId}",
                        userMessage = "Could not connect to sync service. Please check your internet connection.",
                    )
                else ->
                    ErrorResponse(
                        code = "SYNC_FAILED",
                        message = "Synchronization failed",
                        userMessage = "Synchronization failed. Please try again later.",
                    )
            }
        }
        is DeviceSynchronizationContractError.BusinessError.ConflictResolutionFailed ->
            ErrorResponse(
                code = "SYNC_CONFLICT",
                message = "Conflict resolution failed",
                userMessage = "A synchronization conflict was detected. Please resolve the conflict and try again.",
            )
        is DeviceSynchronizationContractError.SystemError.ServiceUnavailable ->
            ErrorResponse(
                code = "SYNC_SERVICE_UNAVAILABLE",
                message = "Sync service unavailable: ${error.service}",
                userMessage = "The synchronization service is temporarily unavailable. Please try again later.",
            )
        else ->
            ErrorResponse(
                code = "SYNC_ERROR",
                message = "Sync error: $error",
                userMessage = "An error occurred during synchronization",
            )
    }

    /**
     * Maps system errors.
     */
    fun mapSystemError(error: ScopeContractError.SystemError): ErrorResponse = when (error) {
        is ScopeContractError.SystemError.ServiceUnavailable ->
            ErrorResponse(
                code = "SERVICE_UNAVAILABLE",
                message = "Service unavailable: ${error.service}",
                userMessage = "The ${error.service} service is temporarily unavailable. Please try again later",
            )
        is ScopeContractError.SystemError.Timeout ->
            ErrorResponse(
                code = "TIMEOUT",
                message = "Operation timeout: ${error.operation}",
                userMessage = "The operation '${error.operation}' timed out after ${error.timeout}",
            )
        is ScopeContractError.SystemError.ConcurrentModification ->
            ErrorResponse(
                code = "CONCURRENT_MODIFICATION",
                message = "Concurrent modification detected",
                userMessage = "The scope was modified by another process. Please reload and try again",
            )
    }

    /**
     * Maps domain validation errors to user-friendly messages.
     */
    fun mapDomainValidationError(error: DomainValidationError): ErrorResponse = when (error) {
        is DomainValidationError.InvalidULID ->
            ErrorResponse(
                code = "INVALID_ULID",
                message = "Invalid ULID format",
                userMessage = "The provided ID '${error.value}' is not a valid ULID format",
                details = mapOf("value" to error.value),
            )
        is DomainValidationError.InvalidPagination.OffsetTooSmall ->
            ErrorResponse(
                code = "INVALID_OFFSET",
                message = "Offset too small",
                userMessage = "Offset must be at least ${error.minOffset}, but was ${error.offset}",
                details = mapOf("offset" to error.offset, "minOffset" to error.minOffset),
            )
        is DomainValidationError.InvalidPagination.LimitTooSmall ->
            ErrorResponse(
                code = "INVALID_LIMIT",
                message = "Limit too small",
                userMessage = "Limit must be at least ${error.minLimit}, but was ${error.limit}",
                details = mapOf("limit" to error.limit, "minLimit" to error.minLimit),
            )
        is DomainValidationError.InvalidPagination.LimitTooLarge ->
            ErrorResponse(
                code = "INVALID_LIMIT",
                message = "Limit too large",
                userMessage = "Limit must not exceed ${error.maxLimit}, but was ${error.limit}",
                details = mapOf("limit" to error.limit, "maxLimit" to error.maxLimit),
            )
        is DomainValidationError.EmptyField ->
            ErrorResponse(
                code = "EMPTY_FIELD",
                message = "Required field is empty",
                userMessage = "${error.fieldName} cannot be empty",
                details = mapOf("fieldName" to error.fieldName),
            )
        is DomainValidationError.InvalidLength ->
            ErrorResponse(
                code = "INVALID_LENGTH",
                message = "Field length out of range",
                userMessage = "${error.fieldName} must be between ${error.minLength} and ${error.maxLength} characters, but was ${error.actualLength}",
                details = mapOf(
                    "fieldName" to error.fieldName,
                    "actualLength" to error.actualLength,
                    "minLength" to error.minLength,
                    "maxLength" to error.maxLength,
                ),
            )
        is DomainValidationError.InvalidIdempotencyKey ->
            ErrorResponse(
                code = "INVALID_IDEMPOTENCY_KEY",
                message = "Invalid idempotency key",
                userMessage = when (error.reason) {
                    DomainValidationError.InvalidIdempotencyKey.IdempotencyKeyError.TOO_SHORT ->
                        "Idempotency key cannot be empty"
                    DomainValidationError.InvalidIdempotencyKey.IdempotencyKeyError.TOO_LONG ->
                        "Idempotency key cannot exceed 64 characters"
                    DomainValidationError.InvalidIdempotencyKey.IdempotencyKeyError.INVALID_CHARACTERS ->
                        "Idempotency key can only contain letters, numbers, hyphens, and underscores"
                    DomainValidationError.InvalidIdempotencyKey.IdempotencyKeyError.INVALID_FORMAT ->
                        "Idempotency key has an invalid format"
                },
                details = mapOf("key" to error.key, "reason" to error.reason.name),
            )
    }

    /**
     * Maps aspect errors to user-friendly messages.
     */
    fun mapAspectError(error: AspectError): ErrorResponse = when (error) {
        is AspectError.NoAspectsProvided ->
            ErrorResponse(
                code = "NO_ASPECTS",
                message = "No aspects provided",
                userMessage = "At least one aspect must be provided",
            )
        is AspectError.InvalidAspectKey ->
            ErrorResponse(
                code = "INVALID_ASPECT_KEY",
                message = "Invalid aspect key",
                userMessage = when (error.reason) {
                    AspectError.InvalidAspectKey.KeyError.EMPTY ->
                        "Aspect key cannot be empty"
                    AspectError.InvalidAspectKey.KeyError.CONTAINS_INVALID_CHARACTERS ->
                        "Aspect key '${error.key}' contains invalid characters"
                    AspectError.InvalidAspectKey.KeyError.TOO_LONG ->
                        "Aspect key '${error.key}' is too long"
                    AspectError.InvalidAspectKey.KeyError.RESERVED_KEYWORD ->
                        "Aspect key '${error.key}' is a reserved keyword"
                },
                details = mapOf("key" to error.key, "reason" to error.reason.name),
            )
        is AspectError.InvalidAspectValue ->
            ErrorResponse(
                code = "INVALID_ASPECT_VALUE",
                message = "Invalid aspect value",
                userMessage = when (error.reason) {
                    AspectError.InvalidAspectValue.ValueError.EMPTY ->
                        "Aspect value for key '${error.key}' cannot be empty"
                    AspectError.InvalidAspectValue.ValueError.TOO_LONG ->
                        "Aspect value for key '${error.key}' is too long"
                    AspectError.InvalidAspectValue.ValueError.INVALID_FORMAT ->
                        "Aspect value for key '${error.key}' has an invalid format"
                    AspectError.InvalidAspectValue.ValueError.OUT_OF_RANGE ->
                        "Aspect value for key '${error.key}' is out of the allowed range"
                },
                details = mapOf("key" to error.key, "value" to error.value, "reason" to error.reason.name),
            )
        is AspectError.InvalidAspectFormat ->
            ErrorResponse(
                code = "INVALID_ASPECT_FORMAT",
                message = "Invalid aspect format",
                userMessage = when (error.reason) {
                    AspectError.InvalidAspectFormat.FormatError.NO_DELIMITER ->
                        "Aspect entry '${error.entry}' must contain '=' or ':' delimiter"
                    AspectError.InvalidAspectFormat.FormatError.MULTIPLE_DELIMITERS ->
                        "Aspect entry '${error.entry}' contains multiple delimiters"
                    AspectError.InvalidAspectFormat.FormatError.EMPTY_KEY ->
                        "Aspect entry '${error.entry}' has an empty key"
                    AspectError.InvalidAspectFormat.FormatError.EMPTY_VALUE ->
                        "Aspect entry '${error.entry}' has an empty value"
                    AspectError.InvalidAspectFormat.FormatError.MALFORMED ->
                        "Aspect entry '${error.entry}' is malformed"
                },
                details = mapOf("entry" to error.entry, "reason" to error.reason.name),
            )
    }
}

/**
 * Standard error response structure.
 */
internal data class ErrorResponse(val code: String, val message: String, val userMessage: String, val details: Map<String, Any>? = null)
