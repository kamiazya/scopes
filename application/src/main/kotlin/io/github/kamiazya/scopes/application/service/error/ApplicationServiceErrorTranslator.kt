package io.github.kamiazya.scopes.application.service.error

import io.github.kamiazya.scopes.application.usecase.error.CreateScopeError

/**
 * Translator for converting between application service errors and UseCase errors.
 *
 * This service provides translation between different error layers following
 * Clean Architecture principles and maintaining proper error boundaries.
 */
object ApplicationServiceErrorTranslator {

    /**
     * Translates ApplicationValidationError to CreateScopeError.
     */
    fun translateValidationError(validationError: ApplicationValidationError): CreateScopeError {
        return when (validationError) {
            is InputValidationError.MissingRequiredField ->
                CreateScopeError.ValidationFailed(validationError.fieldName, "Required field is missing")

            is InputValidationError.InvalidFieldFormat ->
                CreateScopeError.ValidationFailed(
                    validationError.fieldName,
                    "Invalid format: ${validationError.expectedFormat}"
                )

            is InputValidationError.ValueOutOfRange ->
                CreateScopeError.ValidationFailed(
                    validationError.fieldName,
                    "Value out of range: min ${validationError.minValue}, max ${validationError.maxValue}, got ${validationError.actualValue}"
                )

            is CrossAggregateValidationError.CrossReferenceViolation ->
                CreateScopeError.ValidationFailed(
                    validationError.referenceType,
                    "Failed to verify parent scope"
                )

            is CrossAggregateValidationError.InvariantViolation ->
                CreateScopeError.ValidationFailed(
                    validationError.invariantName,
                    "Cross-aggregate uniqueness check failed"
                )

            is CrossAggregateValidationError.AggregateConsistencyViolation ->
                CreateScopeError.ValidationFailed(
                    validationError.consistencyRule,
                    "Aggregate consistency validation failed"
                )

            is BusinessRuleValidationError.PreconditionViolation ->
                CreateScopeError.ValidationFailed(
                    validationError.operation,
                    "Precondition not met: ${validationError.precondition}"
                )

            is BusinessRuleValidationError.PostconditionViolation ->
                CreateScopeError.ValidationFailed(
                    validationError.operation,
                    "Postcondition violated: ${validationError.postcondition}"
                )

            is AsyncValidationError.ValidationTimeout ->
                CreateScopeError.ValidationFailed(
                    validationError.validationPhase,
                    "Validation timed out after ${validationError.timeoutMillis}ms"
                )

            is AsyncValidationError.ConcurrentValidationConflict ->
                CreateScopeError.ValidationFailed(
                    "concurrency",
                    "Concurrent validation conflict on ${validationError.resource}"
                )

        }
    }

    /**
     * Translates AuthorizationServiceError to CreateScopeError.
     */
    fun translateAuthorizationError(authError: AuthorizationServiceError): CreateScopeError {
        return when (authError) {
            is PermissionDeniedError.PermissionDenied ->
                CreateScopeError.ValidationFailed(
                    "authorization",
                    "Permission denied for ${authError.action} on ${authError.resource}"
                )

            is PermissionDeniedError.InsufficientPermissions ->
                CreateScopeError.ValidationFailed(
                    "authorization",
                    "Insufficient permissions: required role ${authError.requiredRole}, has roles ${authError.userRoles}"
                )

            is PermissionDeniedError.RoleNotFound ->
                CreateScopeError.ValidationFailed(
                    "authorization",
                    "Role not found: ${authError.roleId} for user ${authError.userId}"
                )

            is AuthenticationError.UserNotAuthenticated ->
                CreateScopeError.ValidationFailed(
                    "authentication",
                    "User not authenticated for resource: ${authError.requestedResource}"
                )

            is AuthenticationError.InvalidToken ->
                CreateScopeError.ValidationFailed(
                    "authentication",
                    "Invalid ${authError.tokenType} token: ${authError.reason}"
                )

            is AuthorizationContextError.ResourceNotFound ->
                CreateScopeError.ParentNotFound

            is AuthorizationContextError.InvalidContext ->
                CreateScopeError.ValidationFailed(
                    "context",
                    "Invalid ${authError.contextType} context: missing fields ${authError.missingFields}"
                )

            is PolicyEvaluationError.PolicyViolation ->
                CreateScopeError.ValidationFailed(
                    "policy",
                    "Policy violation in ${authError.policyName}: violated rules ${authError.violatedRules}"
                )

            is PolicyEvaluationError.EvaluationFailure ->
                CreateScopeError.ValidationFailed(
                    "policy",
                    "Policy evaluation failed for ${authError.policyId}: ${authError.reason}"
                )
        }
    }

}
