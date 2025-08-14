package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.error.DomainServiceError
import io.github.kamiazya.scopes.domain.error.ServiceOperationError
import io.github.kamiazya.scopes.domain.error.RepositoryIntegrationError
import io.github.kamiazya.scopes.domain.error.ExternalServiceError
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Test class for DomainServiceError hierarchy.
 *
 * Tests verify that general domain service error types provide appropriate
 * context for operational and infrastructure-related errors.
 */
class DomainServiceErrorTest : DescribeSpec({

    describe("DomainServiceError hierarchy") {

        describe("ServiceOperationError") {
            it("should provide context for service unavailable errors") {
                val error = ServiceOperationError.ServiceUnavailable(
                    serviceName = "ValidationService",
                    reason = "Service is temporarily down",
                    estimatedRecoveryTime = 30.seconds,
                    alternativeService = "BackupValidationService"
                )

                error.shouldBeInstanceOf<ServiceOperationError>()
                error.serviceName shouldBe "ValidationService"
                error.reason shouldBe "Service is temporarily down"
                error.estimatedRecoveryTime shouldBe 30.seconds
                error.alternativeService shouldBe "BackupValidationService"
            }

            it("should provide context for service timeout errors") {
                val error = ServiceOperationError.ServiceTimeout(
                    serviceName = "ValidationService",
                    operation = "validateScopeCreation",
                    timeout = 5.seconds,
                    elapsed = 5500.milliseconds
                )

                error.serviceName shouldBe "ValidationService"
                error.operation shouldBe "validateScopeCreation"
                error.timeout shouldBe 5.seconds
                error.elapsed shouldBe 5500.milliseconds
            }
        }

        describe("RepositoryIntegrationError") {
            it("should provide context for repository operation failures") {
                val cause = RuntimeException("Database unavailable")
                val error = RepositoryIntegrationError.RepositoryOperationFailure(
                    operation = "findHierarchyDepth",
                    repositoryName = "ScopeRepository",
                    cause = cause,
                    retryable = true
                )

                error.shouldBeInstanceOf<RepositoryIntegrationError>()
                error.operation shouldBe "findHierarchyDepth"
                error.repositoryName shouldBe "ScopeRepository"
                error.cause shouldBe cause
                error.retryable shouldBe true
            }
        }

        describe("ExternalServiceError") {
            it("should provide context for external service integration failures") {
                val error = ExternalServiceError.IntegrationFailure(
                    serviceName = "TitleNormalizationService",
                    endpoint = "/api/v1/normalize",
                    statusCode = 503,
                    errorMessage = "Service temporarily unavailable",
                    retryAfter = 60.seconds
                )

                error.shouldBeInstanceOf<ExternalServiceError>()
                error.serviceName shouldBe "TitleNormalizationService"
                error.endpoint shouldBe "/api/v1/normalize"
                error.statusCode shouldBe 503
                error.errorMessage shouldBe "Service temporarily unavailable"
                error.retryAfter shouldBe 60.seconds
            }
        }
    }
})
