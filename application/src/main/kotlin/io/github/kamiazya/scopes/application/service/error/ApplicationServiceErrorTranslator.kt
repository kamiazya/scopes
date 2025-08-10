package io.github.kamiazya.scopes.application.service.error

import io.github.kamiazya.scopes.application.usecase.error.CreateScopeError
import io.github.kamiazya.scopes.application.error.ApplicationError

/**
 * Translator for converting between application service errors and UseCase errors.
 * 
 * This service provides translation between different error layers following
 * Clean Architecture principles and maintaining proper error boundaries.
 * 
 * Based on Serena MCP research on error translation patterns.
 */
object ApplicationServiceErrorTranslator {

    /**
     * Translates ApplicationValidationError to CreateScopeError.
     */
    fun translateValidationError(validationError: ApplicationValidationError): CreateScopeError {
        return when (validationError) {
            is ApplicationValidationError.InputValidationError.MissingRequiredField ->
                CreateScopeError.ValidationFailed(validationError.field, "Required field is missing")
                
            is ApplicationValidationError.InputValidationError.InvalidFieldFormat ->
                CreateScopeError.ValidationFailed(
                    validationError.field, 
                    "Invalid format: ${validationError.expectedFormat}"
                )
                
            is ApplicationValidationError.InputValidationError.FieldConstraintViolation ->
                CreateScopeError.ValidationFailed(
                    validationError.field,
                    "${validationError.constraint} violation: expected ${validationError.expectedValue}, got ${validationError.actualValue}"
                )
                
            is ApplicationValidationError.CrossAggregateValidationError.CrossReferenceViolation ->
                CreateScopeError.ValidationFailed(
                    validationError.referenceType,
                    "Cross-reference violation: ${validationError.violation}"
                )
                
            is ApplicationValidationError.CrossAggregateValidationError.InvariantViolation ->
                CreateScopeError.ValidationFailed(
                    validationError.invariantName,
                    validationError.violationDescription
                )
                
            is ApplicationValidationError.CrossAggregateValidationError.AggregateConsistencyViolation ->
                CreateScopeError.ValidationFailed(
                    validationError.consistencyRule,
                    "Consistency violation in ${validationError.operation}: ${validationError.violationDetails}"
                )
                
            is ApplicationValidationError.BusinessRuleValidationError.PreconditionViolation ->
                CreateScopeError.ValidationFailed(
                    validationError.operation,
                    "Precondition not met: ${validationError.precondition}"
                )
                
            is ApplicationValidationError.BusinessRuleValidationError.PostconditionViolation ->
                CreateScopeError.ValidationFailed(
                    validationError.operation,
                    "Postcondition violated: ${validationError.postcondition}"
                )
                
            is ApplicationValidationError.AsyncValidationError.ValidationTimeout ->
                CreateScopeError.ValidationFailed(
                    validationError.validationType,
                    "Validation timed out after ${validationError.elapsedMs}ms"
                )
                
            is ApplicationValidationError.AsyncValidationError.ConcurrentModificationDetected ->
                CreateScopeError.ValidationFailed(
                    "concurrency",
                    "Concurrent modification detected on entity ${validationError.entityId}"
                )
                
            is ApplicationValidationError.AsyncValidationError.ExternalServiceValidationFailure ->
                CreateScopeError.ValidationFailed(
                    validationError.validationType,
                    "External validation service '${validationError.serviceName}' failed after ${validationError.retryAttempts} retries"
                )
        }
    }

    /**
     * Translates AuthorizationServiceError to CreateScopeError.
     */
    fun translateAuthorizationError(authError: AuthorizationServiceError): CreateScopeError {
        return when (authError) {
            is AuthorizationServiceError.PermissionDeniedError.PermissionDenied ->
                CreateScopeError.ValidationFailed(
                    "authorization", 
                    "Permission denied: ${authError.reason}"
                )
                
            is AuthorizationServiceError.PermissionDeniedError.InsufficientRoleLevel ->
                CreateScopeError.ValidationFailed(
                    "authorization",
                    "Insufficient role: required ${authError.requiredRole}, has ${authError.currentRole}"
                )
                
            is AuthorizationServiceError.PermissionDeniedError.ResourceAccessDenied ->
                CreateScopeError.ValidationFailed(
                    "authorization",
                    "Access denied to resource ${authError.resourceId} of type ${authError.resourceType}"
                )
                
            is AuthorizationServiceError.AuthenticationError.UserNotAuthenticated ->
                CreateScopeError.ValidationFailed(
                    "authentication",
                    "User not authenticated for operation: ${authError.operation}"
                )
                
            is AuthorizationServiceError.AuthenticationError.TokenExpired ->
                CreateScopeError.ValidationFailed(
                    "authentication",
                    "Authentication token expired for user ${authError.userId}"
                )
                
            is AuthorizationServiceError.AuthenticationError.InvalidCredentials ->
                CreateScopeError.ValidationFailed(
                    "authentication",
                    "Invalid credentials: ${authError.reason}"
                )
                
            is AuthorizationServiceError.ContextError.ResourceNotFound ->
                CreateScopeError.ParentNotFound
                
            is AuthorizationServiceError.ContextError.InvalidContext ->
                CreateScopeError.ValidationFailed(
                    "context",
                    "Invalid context: ${authError.reason}"
                )
                
            is AuthorizationServiceError.ContextError.AmbiguousContext ->
                CreateScopeError.ValidationFailed(
                    "context",
                    "Ambiguous context: ${authError.reason}"
                )
                
            is AuthorizationServiceError.PolicyEvaluationError.PolicyViolation ->
                CreateScopeError.ValidationFailed(
                    "policy",
                    "Policy violation in ${authError.policyName}: ${authError.violationDetails}"
                )
                
            is AuthorizationServiceError.PolicyEvaluationError.PolicyEvaluationTimeout ->
                CreateScopeError.ValidationFailed(
                    "policy",
                    "Policy evaluation timeout for ${authError.policyName}"
                )
                
            is AuthorizationServiceError.PolicyEvaluationError.PolicyNotFound ->
                CreateScopeError.ValidationFailed(
                    "policy",
                    "Policy not found: ${authError.policyName}"
                )
        }
    }

    /**
     * Translates general ApplicationError to CreateScopeError for backwards compatibility.
     */
    fun translateApplicationError(appError: ApplicationError): CreateScopeError {
        return when (appError) {
            is ApplicationError.DomainErrors ->
                CreateScopeError.DomainRuleViolation(appError.errors.head)
                
            is ApplicationError.Repository ->
                when (appError.cause) {
                    is io.github.kamiazya.scopes.domain.error.SaveScopeError ->
                        CreateScopeError.SaveFailure(appError.cause)
                    else ->
                        CreateScopeError.ValidationFailed("repository", "Repository operation failed")
                }
                
            is ApplicationError.UseCaseError.InvalidRequest ->
                CreateScopeError.ValidationFailed("request", appError.message)
                
            is ApplicationError.UseCaseError.AuthorizationFailed ->
                CreateScopeError.ValidationFailed("authorization", appError.reason)
                
            is ApplicationError.UseCaseError.ConcurrencyConflict ->
                CreateScopeError.ValidationFailed("concurrency", appError.message)
                
            is ApplicationError.IntegrationError.ServiceUnavailable ->
                CreateScopeError.ValidationFailed("integration", "Service unavailable: ${appError.serviceName}")
                
            is ApplicationError.IntegrationError.ServiceTimeout ->
                CreateScopeError.ValidationFailed("integration", "Service timeout: ${appError.serviceName}")
                
            is ApplicationError.IntegrationError.InvalidResponse ->
                CreateScopeError.ValidationFailed("integration", "Invalid response from: ${appError.serviceName}")
        }
    }
}