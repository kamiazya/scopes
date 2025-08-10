package io.github.kamiazya.scopes.application.service.error

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
            it("should create PermissionDenied with specific permission details") {
                val error = AuthorizationServiceError.PermissionDeniedError.PermissionDenied(
                    userId = "user-123",
                    operation = "scope:delete",
                    resourceId = "scope-456",
                    requiredPermission = "SCOPE_DELETE",
                    reason = "User does not have delete permission"
                )

                error.userId shouldBe "user-123"
                error.operation shouldBe "scope:delete"
                error.resourceId shouldBe "scope-456"
                error.requiredPermission shouldBe "SCOPE_DELETE"
                error.reason shouldBe "User does not have delete permission"
            }

            it("should create InsufficientRoleLevel") {
                val error = AuthorizationServiceError.PermissionDeniedError.InsufficientRoleLevel(
                    userId = "user-123",
                    currentRole = "MEMBER",
                    requiredRole = "ADMIN",
                    operation = "scope:create",
                    resourceContext = "project:enterprise"
                )

                error.userId shouldBe "user-123"
                error.currentRole shouldBe "MEMBER"
                error.requiredRole shouldBe "ADMIN"
                error.operation shouldBe "scope:create"
                error.resourceContext shouldBe "project:enterprise"
            }

            it("should create ResourceAccessDenied") {
                val error = AuthorizationServiceError.PermissionDeniedError.ResourceAccessDenied(
                    userId = "user-123",
                    resourceId = "scope-456",
                    resourceType = "Scope",
                    accessType = "WRITE",
                    ownershipRequired = true
                )

                error.userId shouldBe "user-123"
                error.resourceId shouldBe "scope-456"
                error.resourceType shouldBe "Scope"
                error.accessType shouldBe "WRITE"
                error.ownershipRequired shouldBe true
            }
        }

        describe("AuthenticationError") {
            it("should create UserNotAuthenticated") {
                val error = AuthorizationServiceError.AuthenticationError.UserNotAuthenticated(
                    operation = "scope:view",
                    endpoint = "/api/scopes/123"
                )

                error.operation shouldBe "scope:view"
                error.endpoint shouldBe "/api/scopes/123"
            }

            it("should create TokenExpired") {
                val error = AuthorizationServiceError.AuthenticationError.TokenExpired(
                    userId = "user-123",
                    tokenType = "ACCESS",
                    expiredAt = 1640995200000L,
                    currentTime = 1640995300000L
                )

                error.userId shouldBe "user-123"
                error.tokenType shouldBe "ACCESS"
                error.expiredAt shouldBe 1640995200000L
                error.currentTime shouldBe 1640995300000L
            }

            it("should create InvalidCredentials") {
                val error = AuthorizationServiceError.AuthenticationError.InvalidCredentials(
                    credentialType = "API_KEY",
                    reason = "Invalid signature"
                )

                error.credentialType shouldBe "API_KEY"
                error.reason shouldBe "Invalid signature"
            }
        }

        describe("ContextError") {
            it("should create ResourceNotFound") {
                val error = AuthorizationServiceError.ContextError.ResourceNotFound(
                    resourceId = "scope-123",
                    resourceType = "Scope",
                    operation = "scope:view"
                )

                error.resourceId shouldBe "scope-123"
                error.resourceType shouldBe "Scope"
                error.operation shouldBe "scope:view"
            }

            it("should create InvalidContext") {
                val error = AuthorizationServiceError.ContextError.InvalidContext(
                    contextType = "PROJECT",
                    contextId = "project-456",
                    reason = "Project is archived",
                    operation = "scope:create"
                )

                error.contextType shouldBe "PROJECT"
                error.contextId shouldBe "project-456"
                error.reason shouldBe "Project is archived"
                error.operation shouldBe "scope:create"
            }

            it("should create AmbiguousContext") {
                val error = AuthorizationServiceError.ContextError.AmbiguousContext(
                    possibleContexts = listOf("project-1", "project-2"),
                    operation = "scope:create",
                    reason = "Multiple valid contexts found"
                )

                error.possibleContexts shouldBe listOf("project-1", "project-2")
                error.operation shouldBe "scope:create"
                error.reason shouldBe "Multiple valid contexts found"
            }
        }

        describe("PolicyEvaluationError") {
            it("should create PolicyViolation") {
                val error = AuthorizationServiceError.PolicyEvaluationError.PolicyViolation(
                    policyName = "hierarchyDepthPolicy",
                    policyVersion = "v1.2.0",
                    violationDetails = "Maximum depth of 10 exceeded",
                    operation = "scope:create",
                    resourceId = "scope-deep-nested"
                )

                error.policyName shouldBe "hierarchyDepthPolicy"
                error.policyVersion shouldBe "v1.2.0"
                error.violationDetails shouldBe "Maximum depth of 10 exceeded"
                error.operation shouldBe "scope:create"
                error.resourceId shouldBe "scope-deep-nested"
            }

            it("should create PolicyEvaluationTimeout") {
                val error = AuthorizationServiceError.PolicyEvaluationError.PolicyEvaluationTimeout(
                    policyName = "complexBusinessRulePolicy",
                    timeoutMs = 5000,
                    elapsedMs = 5100,
                    operation = "scope:update"
                )

                error.policyName shouldBe "complexBusinessRulePolicy"
                error.timeoutMs shouldBe 5000
                error.elapsedMs shouldBe 5100
                error.operation shouldBe "scope:update"
            }

            it("should create PolicyNotFound") {
                val error = AuthorizationServiceError.PolicyEvaluationError.PolicyNotFound(
                    policyName = "nonExistentPolicy",
                    operation = "scope:delete",
                    availablePolicies = listOf("scopeHierarchyPolicy", "scopeOwnershipPolicy")
                )

                error.policyName shouldBe "nonExistentPolicy"
                error.operation shouldBe "scope:delete"
                error.availablePolicies shouldBe listOf("scopeHierarchyPolicy", "scopeOwnershipPolicy")
            }
        }

        describe("error hierarchy") {
            it("all errors should extend AuthorizationServiceError") {
                val permissionError = AuthorizationServiceError.PermissionDeniedError.PermissionDenied(
                    "user", "op", "resource", "permission", "reason"
                )
                val authError = AuthorizationServiceError.AuthenticationError.UserNotAuthenticated("op", "endpoint")
                val contextError = AuthorizationServiceError.ContextError.ResourceNotFound("id", "type", "op")
                val policyError = AuthorizationServiceError.PolicyEvaluationError.PolicyNotFound(
                    "policy", "op", emptyList()
                )

                permissionError should beInstanceOf<AuthorizationServiceError>()
                authError should beInstanceOf<AuthorizationServiceError>()
                contextError should beInstanceOf<AuthorizationServiceError>()
                policyError should beInstanceOf<AuthorizationServiceError>()
            }
        }
    }
})