package io.github.kamiazya.scopes.application.service.error

import kotlinx.datetime.Instant

/**
 * Authorization service errors for access control and permission violations.
 *
 * This hierarchy provides comprehensive error types for authorization concerns
 * including permission management, authentication, context validation, and policy evaluation.
 *
 * Based on Serena MCP research on authorization patterns:
 * - Role-based access control with fine-grained permissions
 * - Context-aware authorization with resource validation
 * - Policy-based authorization with rule evaluation
 * - Clear separation between authentication and authorization
 *
 * Following functional error handling principles for composability.
 */
sealed class AuthorizationServiceError

/**
 * Permission denied errors for access control violations.
 * These represent cases where authentication is valid but authorization fails.
 */
sealed class PermissionDeniedError : AuthorizationServiceError() {

    /**
     * General permission denied with specific details.
     */
    data class PermissionDenied(
        val resource: String,
        val action: String,
        val userId: String,
        val requiredPermissions: List<String>,
        val actualPermissions: List<String>,
    ) : PermissionDeniedError()

    /**
     * Insufficient permissions for the requested operation.
     */
    data class InsufficientPermissions(
        val operation: String,
        val requiredRole: String,
        val userRoles: List<String>,
        val additionalContext: Map<String, String>? = null,
    ) : PermissionDeniedError()

    /**
     * Role not found or invalid for the user.
     */
    data class RoleNotFound(
        val roleId: String,
        val userId: String,
        val availableRoles: List<String>,
        val context: String,
    ) : PermissionDeniedError()
}

/**
 * Authentication errors for identity verification failures.
 * These represent cases where user identity cannot be established.
 */
sealed class AuthenticationError : AuthorizationServiceError() {

    /**
     * User is not authenticated.
     */
    data class UserNotAuthenticated(
        val requestedResource: String,
        val authenticationMethod: String?,
        val redirectUrl: String? = null,
    ) : AuthenticationError()

    /**
     * Authentication token is invalid or expired.
     */
    data class InvalidToken(
        val tokenType: String,
        val reason: String,
        val expiresAt: Instant? = null,
        val refreshable: Boolean = false,
    ) : AuthenticationError()
}

/**
 * Context errors for authorization context validation failures.
 * These represent problems with the authorization context itself.
 */
sealed class AuthorizationContextError : AuthorizationServiceError() {

    /**
     * Resource required for authorization check does not exist.
     */
    data class ResourceNotFound(
        val resourceId: String,
        val resourceType: String,
        val operation: String,
        val searchContext: Map<String, String>? = null,
    ) : AuthorizationContextError()

    /**
     * Invalid authorization context provided.
     */
    data class InvalidContext(
        val contextType: String,
        val missingFields: List<String>,
        val invalidFields: Map<String, String>,
        val requiredContext: String,
    ) : AuthorizationContextError()
}

/**
 * Policy evaluation errors for policy-based authorization failures.
 * These handle failures in policy-based authorization systems.
 */
sealed class PolicyEvaluationError : AuthorizationServiceError() {

    /**
     * Authorization policy was violated.
     */
    data class PolicyViolation(
        val policyId: String,
        val policyName: String,
        val evaluationResult: String,
        val violatedRules: List<String>,
        val context: Map<String, Any>,
    ) : PolicyEvaluationError()

    /**
     * Policy evaluation failed due to system error.
     */
    data class EvaluationFailure(
        val policyId: String,
        val reason: String,
        val cause: Throwable,
        val fallbackBehavior: String? = null,
    ) : PolicyEvaluationError()
}
