package io.github.kamiazya.scopes.interfaces.cli.mappers

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError

/**
 * Maps domain and contract errors to user-friendly messages for CLI output.
 */
object ErrorMessageMapper {
    /**
     * Maps any error to a user-friendly message.
     */
    fun toUserMessage(error: Any): String = when (error) {
        is ScopeContractError -> getMessage(error)
        else -> mapGenericError(error)
    }

    private fun mapGenericError(error: Any): String {
        val errorString = error.toString()
        return when {
            errorString.contains("NotFound") -> "The requested item was not found"
            errorString.contains("AlreadyExists") -> "The item already exists"
            errorString.contains("Invalid") -> "Invalid input provided"
            errorString.contains("Conflict") -> "Operation conflicts with current state"
            errorString.contains("Unavailable") -> "Service temporarily unavailable"
            else -> "An error occurred: $error"
        }
    }

    /**
     * Maps contract errors to user-friendly messages.
     */
    fun getMessage(error: ScopeContractError): String = when (error) {
        is ScopeContractError.InputError -> mapInputError(error)
        is ScopeContractError.BusinessError -> mapBusinessError(error)
        is ScopeContractError.DataInconsistency.MissingCanonicalAlias ->
            "Data inconsistency: Scope ${error.scopeId} is missing its canonical alias. Contact administrator to rebuild aliases."
        is ScopeContractError.SystemError -> mapSystemError(error)
    }

    private fun mapInputError(error: ScopeContractError.InputError): String = when (error) {
        is ScopeContractError.InputError.InvalidId -> formatInvalidId(error)
        is ScopeContractError.InputError.InvalidTitle -> formatInvalidTitle(error)
        is ScopeContractError.InputError.InvalidDescription -> formatInvalidDescription(error)
        is ScopeContractError.InputError.InvalidParentId -> formatInvalidParentId(error)
        is ScopeContractError.InputError.InvalidAlias -> formatInvalidAlias(error)
        is ScopeContractError.InputError.InvalidContextKey -> formatInvalidContextKey(error)
        is ScopeContractError.InputError.InvalidContextName -> formatInvalidContextName(error)
        is ScopeContractError.InputError.InvalidContextFilter -> formatInvalidContextFilter(error)
    }

    private fun formatInvalidId(error: ScopeContractError.InputError.InvalidId): String =
        "Invalid ID format: ${error.id}${error.expectedFormat?.let { " (expected: $it)" } ?: ""}"

    private fun formatInvalidTitle(error: ScopeContractError.InputError.InvalidTitle): String {
        val failure = error.validationFailure
        return when (failure) {
            is ScopeContractError.TitleValidationFailure.TooShort ->
                "Title too short: minimum ${failure.minimumLength} characters"
            is ScopeContractError.TitleValidationFailure.TooLong ->
                "Title too long: maximum ${failure.maximumLength} characters"
            else -> ValidationMessageFormatter.formatTitleValidationFailure(failure)
        }
    }

    private fun formatInvalidDescription(error: ScopeContractError.InputError.InvalidDescription): String {
        val failure = error.validationFailure as ScopeContractError.DescriptionValidationFailure.TooLong
        return "Description too long: maximum ${failure.maximumLength} characters"
    }

    private fun formatInvalidParentId(error: ScopeContractError.InputError.InvalidParentId): String =
        "Invalid parent ID: ${error.parentId}${error.expectedFormat?.let { " (expected: $it)" } ?: ""}"

    private fun formatInvalidAlias(error: ScopeContractError.InputError.InvalidAlias): String {
        val failure = error.validationFailure
        return when (failure) {
            is ScopeContractError.AliasValidationFailure.TooShort ->
                "Alias too short: minimum ${failure.minimumLength} characters"
            is ScopeContractError.AliasValidationFailure.TooLong ->
                "Alias too long: maximum ${failure.maximumLength} characters"
            else -> ValidationMessageFormatter.formatAliasValidationFailure(failure)
        }
    }

    private fun formatInvalidContextKey(error: ScopeContractError.InputError.InvalidContextKey): String {
        val failure = error.validationFailure
        return when (failure) {
            is ScopeContractError.ContextKeyValidationFailure.TooShort ->
                "Context key too short: minimum ${failure.minimumLength} characters"
            is ScopeContractError.ContextKeyValidationFailure.TooLong ->
                "Context key too long: maximum ${failure.maximumLength} characters"
            is ScopeContractError.ContextKeyValidationFailure.InvalidFormat ->
                "Invalid context key format: ${failure.invalidType}"
            else -> ValidationMessageFormatter.formatContextKeyValidationFailure(failure)
        }
    }

    private fun formatInvalidContextName(error: ScopeContractError.InputError.InvalidContextName): String {
        val failure = error.validationFailure
        return if (failure is ScopeContractError.ContextNameValidationFailure.TooLong) {
            "Context name too long: maximum ${failure.maximumLength} characters"
        } else {
            ValidationMessageFormatter.formatContextNameValidationFailure(failure)
        }
    }

    private fun formatInvalidContextFilter(error: ScopeContractError.InputError.InvalidContextFilter): String {
        val failure = error.validationFailure
        return when (failure) {
            is ScopeContractError.ContextFilterValidationFailure.TooShort ->
                "Context filter too short: minimum ${failure.minimumLength} characters"
            is ScopeContractError.ContextFilterValidationFailure.TooLong ->
                "Context filter too long: maximum ${failure.maximumLength} characters"
            else -> ValidationMessageFormatter.formatContextFilterValidationFailure(failure)
        }
    }

    private fun mapBusinessError(error: ScopeContractError.BusinessError): String = when (error) {
        is ScopeContractError.BusinessError.NotFound -> "Not found: ${error.scopeId}"
        is ScopeContractError.BusinessError.DuplicateTitle -> formatDuplicateTitle(error)
        is ScopeContractError.BusinessError.HierarchyViolation -> formatHierarchyViolation(error)
        is ScopeContractError.BusinessError.AlreadyDeleted -> "Already deleted: ${error.scopeId}"
        is ScopeContractError.BusinessError.ArchivedScope -> "Cannot modify archived scope: ${error.scopeId}"
        is ScopeContractError.BusinessError.NotArchived -> "Scope is not archived: ${error.scopeId}"
        is ScopeContractError.BusinessError.HasChildren -> formatHasChildren(error)
        is ScopeContractError.BusinessError.AliasNotFound -> "Alias not found: ${error.alias}"
        is ScopeContractError.BusinessError.DuplicateAlias -> "Alias already exists: ${error.alias}"
        is ScopeContractError.BusinessError.CannotRemoveCanonicalAlias -> "Cannot remove canonical alias"
        is ScopeContractError.BusinessError.AliasOfDifferentScope -> "Alias '${error.alias}' belongs to different scope"
        is ScopeContractError.BusinessError.AliasGenerationFailed -> "Failed to generate alias (retries: ${error.retryCount})"
        is ScopeContractError.BusinessError.AliasGenerationValidationFailed -> "Generated alias invalid: ${error.reason}"
        is ScopeContractError.BusinessError.ContextNotFound -> "Context not found: ${error.contextKey}"
        is ScopeContractError.BusinessError.DuplicateContextKey -> "Context key already exists: ${error.contextKey}"
    }

    private fun formatDuplicateTitle(error: ScopeContractError.BusinessError.DuplicateTitle): String =
        "Duplicate title '${error.title}'${error.parentId?.let { " under parent $it" } ?: " at root level"}"

    private fun formatHasChildren(error: ScopeContractError.BusinessError.HasChildren): String =
        "Cannot delete scope with children: ${error.scopeId}${error.childrenCount?.let { " ($it children)" } ?: ""}"

    private fun formatHierarchyViolation(error: ScopeContractError.BusinessError.HierarchyViolation): String {
        val violation = error.violation
        return when (violation) {
            is ScopeContractError.HierarchyViolationType.CircularReference ->
                "Circular reference detected: ${violation.scopeId} -> ${violation.parentId}"
            is ScopeContractError.HierarchyViolationType.MaxDepthExceeded ->
                "Maximum depth exceeded: ${violation.attemptedDepth} (max: ${violation.maximumDepth})"
            is ScopeContractError.HierarchyViolationType.MaxChildrenExceeded ->
                "Maximum children exceeded for ${violation.parentId}: ${violation.currentChildrenCount} (max: ${violation.maximumChildren})"
            else -> ValidationMessageFormatter.formatHierarchyViolation(violation)
        }
    }

    private fun mapSystemError(error: ScopeContractError.SystemError): String = when (error) {
        is ScopeContractError.SystemError.ServiceUnavailable ->
            "Service unavailable: ${error.service}"
        is ScopeContractError.SystemError.Timeout ->
            "Operation timeout: ${error.operation} (${error.timeout})"
        is ScopeContractError.SystemError.ConcurrentModification ->
            "Concurrent modification detected for ${error.scopeId} (expected: ${error.expectedVersion}, actual: ${error.actualVersion})"
    }
}
