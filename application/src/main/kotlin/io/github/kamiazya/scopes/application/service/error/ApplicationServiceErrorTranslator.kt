package io.github.kamiazya.scopes.application.service.error

import io.github.kamiazya.scopes.application.usecase.error.CreateScopeError

import io.github.kamiazya.scopes.application.service.error.ApplicationValidationError
import io.github.kamiazya.scopes.application.service.error.AuditServiceError
import io.github.kamiazya.scopes.application.service.error.NotificationServiceError
import io.github.kamiazya.scopes.application.service.error.AuthorizationServiceError
import io.github.kamiazya.scopes.application.service.error.InputValidationError
import io.github.kamiazya.scopes.application.service.error.CrossAggregateValidationError
import io.github.kamiazya.scopes.application.service.error.BusinessRuleValidationError
import io.github.kamiazya.scopes.application.service.error.AsyncValidationError
import io.github.kamiazya.scopes.application.service.error.AuditTrailError
import io.github.kamiazya.scopes.application.service.error.EventLoggingError
import io.github.kamiazya.scopes.application.service.error.ComplianceError
import io.github.kamiazya.scopes.application.service.error.AuditSystemError
import io.github.kamiazya.scopes.application.service.error.MessageDeliveryError
import io.github.kamiazya.scopes.application.service.error.EventDistributionError
import io.github.kamiazya.scopes.application.service.error.TemplateError
import io.github.kamiazya.scopes.application.service.error.NotificationConfigurationError
import io.github.kamiazya.scopes.application.service.error.PermissionDeniedError
import io.github.kamiazya.scopes.application.service.error.AuthenticationError
import io.github.kamiazya.scopes.application.service.error.AuthorizationContextError
import io.github.kamiazya.scopes.application.service.error.PolicyEvaluationError

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
                    "Cross-reference violation: ${validationError.violation}"
                )
                
            is CrossAggregateValidationError.InvariantViolation ->
                CreateScopeError.ValidationFailed(
                    validationError.invariantName,
                    validationError.violationDescription
                )
                
            is CrossAggregateValidationError.AggregateConsistencyViolation ->
                CreateScopeError.ValidationFailed(
                    validationError.consistencyRule,
                    "Consistency violation in ${validationError.operation}: ${validationError.violationDetails}"
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