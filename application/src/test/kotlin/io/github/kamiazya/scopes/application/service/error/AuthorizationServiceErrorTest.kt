package io.github.kamiazya.scopes.application.service.error

import kotlinx.datetime.Instant

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf

/**
 * Test for AuthorizationServiceError hierarchy.
 * 
 * This test validates that authorization service errors provide
 * comprehensive permission and role-based access control error information.
 * 
 * Following Serena MCP guidance on authorization patterns in DDD:
 * - Separation of authorization concerns from domain logic
 * - Type-safe error handling with Arrow Either
 * - Permission-specific error details
 * - Role-based access control validation
 */
class AuthorizationServiceErrorTest : DescribeSpec({

    describe("AuthorizationServiceError hierarchy") {

        describe("PermissionDeniedError") {
            it("should create PermissionDenied") {
                val error = PermissionDeniedError.PermissionDenied(
                    resource = "scope-123",
                    action = "delete",
                    userId = "user-456",
                    requiredPermissions = listOf("SCOPE_DELETE"),
                    actualPermissions = listOf("SCOPE_READ")
                )

                error.resource shouldBe "scope-123"
                error.action shouldBe "delete"
                error.userId shouldBe "user-456"
                error.requiredPermissions shouldBe listOf("SCOPE_DELETE")
                error.actualPermissions shouldBe listOf("SCOPE_READ")
            }

            it("should create InsufficientPermissions") {
                val error = PermissionDeniedError.InsufficientPermissions(
                    operation = "scope:create",
                    requiredRole = "ADMIN",
                    userRoles = listOf("MEMBER"),
                    additionalContext = mapOf("project" to "test-project")
                )

                error.operation shouldBe "scope:create"
                error.requiredRole shouldBe "ADMIN"
                error.userRoles shouldBe listOf("MEMBER")
                error.additionalContext shouldBe mapOf("project" to "test-project")
            }
        }

        describe("AuthenticationError") {
            it("should create UserNotAuthenticated") {
                val error = AuthenticationError.UserNotAuthenticated(
                    requestedResource = "scope:view",
                    authenticationMethod = "JWT"
                )

                error.requestedResource shouldBe "scope:view"
                error.authenticationMethod shouldBe "JWT"
            }

            it("should create InvalidToken") {
                val error = AuthenticationError.InvalidToken(
                    tokenType = "ACCESS",
                    reason = "Token expired",
                    expiresAt = Instant.fromEpochMilliseconds(1640995200000L),
                    refreshable = true
                )

                error.tokenType shouldBe "ACCESS"
                error.reason shouldBe "Token expired"
                error.expiresAt shouldBe Instant.fromEpochMilliseconds(1640995200000L)
                error.refreshable shouldBe true
            }
        }

        describe("AuthorizationContextError") {
            it("should create ResourceNotFound") {
                val error = AuthorizationContextError.ResourceNotFound(
                    resourceId = "scope-123",
                    resourceType = "Scope",
                    operation = "scope:view"
                )

                error.resourceId shouldBe "scope-123"
                error.resourceType shouldBe "Scope"
                error.operation shouldBe "scope:view"
            }

            it("should create InvalidContext") {
                val error = AuthorizationContextError.InvalidContext(
                    contextType = "PROJECT",
                    missingFields = listOf("projectId"),
                    invalidFields = mapOf("status" to "archived"),
                    requiredContext = "Active project context"
                )

                error.contextType shouldBe "PROJECT"
                error.missingFields shouldBe listOf("projectId")
                error.invalidFields shouldBe mapOf("status" to "archived")
                error.requiredContext shouldBe "Active project context"
            }
        }

        describe("PolicyEvaluationError") {
            it("should create PolicyViolation") {
                val error = PolicyEvaluationError.PolicyViolation(
                    policyId = "policy-123",
                    policyName = "hierarchyDepthPolicy",
                    evaluationResult = "DENY",
                    violatedRules = listOf("max_depth_exceeded"),
                    context = mapOf("depth" to 15)
                )

                error.policyId shouldBe "policy-123"
                error.policyName shouldBe "hierarchyDepthPolicy"
                error.evaluationResult shouldBe "DENY"
                error.violatedRules shouldBe listOf("max_depth_exceeded")
                error.context shouldBe mapOf("depth" to 15)
            }

            it("should create EvaluationFailure") {
                val cause = RuntimeException("Policy engine error")
                val error = PolicyEvaluationError.EvaluationFailure(
                    policyId = "policy-456",
                    reason = "Policy engine unavailable",
                    cause = cause,
                    fallbackBehavior = "DENY_ALL"
                )

                error.policyId shouldBe "policy-456"
                error.reason shouldBe "Policy engine unavailable"
                error.cause shouldBe cause
                error.fallbackBehavior shouldBe "DENY_ALL"
            }
        }

        describe("error hierarchy") {
            it("all errors should extend AuthorizationServiceError") {
                val permissionError = PermissionDeniedError.PermissionDenied(
                    resource = "test",
                    action = "test",
                    userId = "test",
                    requiredPermissions = emptyList(),
                    actualPermissions = emptyList()
                )
                val authError = AuthenticationError.UserNotAuthenticated(
                    requestedResource = "test",
                    authenticationMethod = "test"
                )
                val contextError = AuthorizationContextError.ResourceNotFound(
                    resourceId = "test",
                    resourceType = "test",
                    operation = "test"
                )
                val policyError = PolicyEvaluationError.PolicyViolation(
                    policyId = "test",
                    policyName = "test",
                    evaluationResult = "test",
                    violatedRules = emptyList(),
                    context = emptyMap()
                )

                permissionError should beInstanceOf<AuthorizationServiceError>()
                authError should beInstanceOf<AuthorizationServiceError>()
                contextError should beInstanceOf<AuthorizationServiceError>()
                policyError should beInstanceOf<AuthorizationServiceError>()
            }
        }
    }
})