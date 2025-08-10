package io.github.kamiazya.scopes.application.service.error

/**
 * Authorization service errors for permission and access control failures.
 * 
 * This hierarchy provides comprehensive error types for authorization concerns
 * that are separated from core domain logic as recommended by Serena MCP research.
 * 
 * Based on DDD authorization patterns:
 * - Clear separation of authorization concerns from domain logic
 * - Type-safe error handling with Arrow Either
 * - Comprehensive permission and role-based access control errors
 * - Policy evaluation and context validation errors
 * 
 * Following functional error handling principles for composability.
 */
sealed class AuthorizationServiceError {

    /**
     * Permission denied errors for access control violations.
     * These represent cases where authentication is valid but authorization fails.
     */
    sealed class PermissionDeniedError : AuthorizationServiceError() {
        
        /**
         * General permission denied with specific details.
         */
        data class PermissionDenied(
            val userId: String,
            val operation: String,
            val resourceId: String,
            val requiredPermission: String,
            val reason: String
        ) : PermissionDeniedError()
        
        /**
         * User's role level is insufficient for the operation.
         */
        data class InsufficientRoleLevel(
            val userId: String,
            val currentRole: String,
            val requiredRole: String,
            val operation: String,
            val resourceContext: String
        ) : PermissionDeniedError()
        
        /**
         * Access denied to specific resource based on ownership or access rules.
         */
        data class ResourceAccessDenied(
            val userId: String,
            val resourceId: String,
            val resourceType: String,
            val accessType: String,
            val ownershipRequired: Boolean
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
            val operation: String,
            val endpoint: String
        ) : AuthenticationError()
        
        /**
         * Authentication token has expired.
         */
        data class TokenExpired(
            val userId: String,
            val tokenType: String,
            val expiredAt: Long,
            val currentTime: Long
        ) : AuthenticationError()
        
        /**
         * Provided credentials are invalid.
         */
        data class InvalidCredentials(
            val credentialType: String,
            val reason: String
        ) : AuthenticationError()
    }

    /**
     * Context errors for resource and context resolution issues.
     * These represent problems with the authorization context itself.
     */
    sealed class ContextError : AuthorizationServiceError() {
        
        /**
         * Resource required for authorization check does not exist.
         */
        data class ResourceNotFound(
            val resourceId: String,
            val resourceType: String,
            val operation: String
        ) : ContextError()
        
        /**
         * Authorization context is invalid or corrupted.
         */
        data class InvalidContext(
            val contextType: String,
            val contextId: String,
            val reason: String,
            val operation: String
        ) : ContextError()
        
        /**
         * Multiple contexts match, causing ambiguity in authorization.
         */
        data class AmbiguousContext(
            val possibleContexts: List<String>,
            val operation: String,
            val reason: String
        ) : ContextError()
    }

    /**
     * Policy evaluation errors for complex authorization rules.
     * These handle failures in policy-based authorization systems.
     */
    sealed class PolicyEvaluationError : AuthorizationServiceError() {
        
        /**
         * Authorization policy was violated.
         */
        data class PolicyViolation(
            val policyName: String,
            val policyVersion: String,
            val violationDetails: String,
            val operation: String,
            val resourceId: String
        ) : PolicyEvaluationError()
        
        /**
         * Policy evaluation timed out.
         */
        data class PolicyEvaluationTimeout(
            val policyName: String,
            val timeoutMs: Long,
            val elapsedMs: Long,
            val operation: String
        ) : PolicyEvaluationError()
        
        /**
         * Required authorization policy was not found.
         */
        data class PolicyNotFound(
            val policyName: String,
            val operation: String,
            val availablePolicies: List<String>
        ) : PolicyEvaluationError()
    }
}