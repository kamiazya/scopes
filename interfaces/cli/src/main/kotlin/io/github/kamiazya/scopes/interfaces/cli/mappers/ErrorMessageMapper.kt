package io.github.kamiazya.scopes.interfaces.cli.mappers

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError

/**
 * Maps domain and contract errors to user-friendly messages for CLI output.
 */
object ErrorMessageMapper {
    private val inputErrorMapper = InputErrorMapper()
    private val businessErrorMapper = BusinessErrorMapper()
    private val systemErrorMapper = SystemErrorMapper()
    private val genericErrorMapper = GenericErrorMapper()

    /**
     * Maps any error to a user-friendly message.
     */
    fun toUserMessage(error: Any): String = when (error) {
        is ScopeContractError -> getMessage(error)
        else -> genericErrorMapper.mapGenericError(error)
    }

    /**
     * Maps contract errors to user-friendly messages.
     */
    fun getMessage(error: ScopeContractError): String = when (error) {
        is ScopeContractError.InputError -> inputErrorMapper.mapInputError(error)
        is ScopeContractError.BusinessError -> businessErrorMapper.mapBusinessError(error)
        is ScopeContractError.DataInconsistency.MissingCanonicalAlias ->
            "Data inconsistency: Scope ${error.scopeId} is missing its canonical alias. Contact administrator to rebuild aliases."
        is ScopeContractError.SystemError -> systemErrorMapper.mapSystemError(error)
    }
}

/**
 * Specialized mapper for input validation errors.
 */
internal class InputErrorMapper {
    fun mapInputError(error: ScopeContractError.InputError): String = when (error) {
        is ScopeContractError.InputError.InvalidId ->
            "Invalid ID format: ${error.id}${error.expectedFormat?.let { " (expected: $it)" } ?: ""}"
        is ScopeContractError.InputError.InvalidTitle -> formatTitleError(error)
        is ScopeContractError.InputError.InvalidDescription -> formatDescriptionError(error)
        is ScopeContractError.InputError.InvalidParentId ->
            "Invalid parent ID: ${error.parentId}${error.expectedFormat?.let { " (expected: $it)" } ?: ""}"
        is ScopeContractError.InputError.InvalidAlias -> formatAliasError(error)
        is ScopeContractError.InputError.InvalidContextKey -> formatContextKeyError(error)
        is ScopeContractError.InputError.InvalidContextName -> formatContextNameError(error)
        is ScopeContractError.InputError.InvalidContextFilter -> formatContextFilterError(error)
        is ScopeContractError.InputError.ValidationFailure -> formatValidationFailureError(error)
    }

    private fun formatTitleError(error: ScopeContractError.InputError.InvalidTitle): String {
        val failure = error.validationFailure
        return when (failure) {
            is ScopeContractError.TitleValidationFailure.TooShort ->
                "Title too short: minimum ${failure.minimumLength} characters"
            is ScopeContractError.TitleValidationFailure.TooLong ->
                "Title too long: maximum ${failure.maximumLength} characters"
            else -> ValidationMessageFormatter.formatTitleValidationFailure(failure)
        }
    }

    private fun formatDescriptionError(error: ScopeContractError.InputError.InvalidDescription): String =
        "Description too long: maximum ${(error.validationFailure as ScopeContractError.DescriptionValidationFailure.TooLong).maximumLength} characters"

    private fun formatAliasError(error: ScopeContractError.InputError.InvalidAlias): String {
        val failure = error.validationFailure
        return when (failure) {
            is ScopeContractError.AliasValidationFailure.TooShort ->
                "Alias too short: minimum ${failure.minimumLength} characters"
            is ScopeContractError.AliasValidationFailure.TooLong ->
                "Alias too long: maximum ${failure.maximumLength} characters"
            else -> ValidationMessageFormatter.formatAliasValidationFailure(failure)
        }
    }

    private fun formatContextKeyError(error: ScopeContractError.InputError.InvalidContextKey): String {
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

    private fun formatContextNameError(error: ScopeContractError.InputError.InvalidContextName): String {
        val failure = error.validationFailure
        return if (failure is ScopeContractError.ContextNameValidationFailure.TooLong) {
            "Context name too long: maximum ${failure.maximumLength} characters"
        } else {
            ValidationMessageFormatter.formatContextNameValidationFailure(failure)
        }
    }

    private fun formatContextFilterError(error: ScopeContractError.InputError.InvalidContextFilter): String {
        val failure = error.validationFailure
        return when (failure) {
            is ScopeContractError.ContextFilterValidationFailure.TooShort ->
                "Context filter too short: minimum ${failure.minimumLength} characters"
            is ScopeContractError.ContextFilterValidationFailure.TooLong ->
                "Context filter too long: maximum ${failure.maximumLength} characters"
            else -> ValidationMessageFormatter.formatContextFilterValidationFailure(failure)
        }
    }

    private fun formatValidationFailureError(error: ScopeContractError.InputError.ValidationFailure): String {
        val constraintMessage = getConstraintMessage(error.constraint)
        return "${error.field.replaceFirstChar { it.uppercase() }} $constraintMessage"
    }

    private fun getConstraintMessage(constraint: ScopeContractError.ValidationConstraint): String = when (constraint) {
        is ScopeContractError.ValidationConstraint.Empty -> "must not be empty"
        is ScopeContractError.ValidationConstraint.TooShort ->
            "too short: minimum ${constraint.minimumLength} characters"
        is ScopeContractError.ValidationConstraint.TooLong ->
            "too long: maximum ${constraint.maximumLength} characters"
        is ScopeContractError.ValidationConstraint.InvalidFormat ->
            "invalid format"
        is ScopeContractError.ValidationConstraint.InvalidType ->
            "invalid type: expected ${constraint.expectedType}"
        is ScopeContractError.ValidationConstraint.InvalidValue ->
            "invalid value: ${constraint.actualValue}" +
                (constraint.expectedValues?.run { " (allowed: ${joinToString(", ")})" } ?: "")
        is ScopeContractError.ValidationConstraint.EmptyValues ->
            "cannot be empty"
        is ScopeContractError.ValidationConstraint.MultipleValuesNotAllowed ->
            "multiple values not allowed"
        is ScopeContractError.ValidationConstraint.RequiredField ->
            "is required"
    }
}

/**
 * Specialized mapper for business logic errors.
 */
internal class BusinessErrorMapper {
    fun mapBusinessError(error: ScopeContractError.BusinessError): String = when (error) {
        is ScopeContractError.BusinessError.NotFound -> "Not found: ${error.scopeId}"
        is ScopeContractError.BusinessError.DuplicateTitle ->
            "Duplicate title '${error.title}'${error.parentId?.let { " under parent $it" } ?: " at root level"}"
        is ScopeContractError.BusinessError.HierarchyViolation -> formatHierarchyViolation(error)
        is ScopeContractError.BusinessError.AlreadyDeleted -> "Already deleted: ${error.scopeId}"
        is ScopeContractError.BusinessError.ArchivedScope -> "Cannot modify archived scope: ${error.scopeId}"
        is ScopeContractError.BusinessError.NotArchived -> "Scope is not archived: ${error.scopeId}"
        is ScopeContractError.BusinessError.HasChildren ->
            "Cannot delete scope with children: ${error.scopeId}${error.childrenCount?.let { " ($it children)" } ?: ""}"
        is ScopeContractError.BusinessError.AliasNotFound -> "Alias not found: ${error.alias}"
        is ScopeContractError.BusinessError.DuplicateAlias -> "Alias already exists: ${error.alias}"
        is ScopeContractError.BusinessError.CannotRemoveCanonicalAlias ->
            "Cannot remove canonical alias"
        is ScopeContractError.BusinessError.AliasOfDifferentScope ->
            "Alias '${error.alias}' belongs to different scope"
        is ScopeContractError.BusinessError.AliasGenerationFailed ->
            "Failed to generate alias (retries: ${error.retryCount})"
        is ScopeContractError.BusinessError.AliasGenerationValidationFailed ->
            "Generated alias invalid: ${error.reason}"
        is ScopeContractError.BusinessError.ContextNotFound -> "Context not found: ${error.contextKey}"
        is ScopeContractError.BusinessError.DuplicateContextKey -> "Context key already exists: ${error.contextKey}"
    }

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
}

/**
 * Specialized mapper for system errors.
 */
internal class SystemErrorMapper {
    fun mapSystemError(error: ScopeContractError.SystemError): String = when (error) {
        is ScopeContractError.SystemError.ServiceUnavailable ->
            "Service unavailable: ${error.service}"
        is ScopeContractError.SystemError.Timeout ->
            "Operation timeout: ${error.operation} (${error.timeout})"
        is ScopeContractError.SystemError.ConcurrentModification ->
            "Concurrent modification detected for ${error.scopeId} (expected: ${error.expectedVersion}, actual: ${error.actualVersion})"
    }
}

/**
 * Specialized mapper for generic errors.
 */
internal class GenericErrorMapper {
    fun mapGenericError(error: Any): String {
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
}
