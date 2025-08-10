package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.error.DomainServiceError
import io.github.kamiazya.scopes.domain.error.RepositoryError
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

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
                val error = DomainServiceError.ServiceOperationError.ServiceUnavailable(
                    serviceName = "ValidationService",
                    reason = "Service is temporarily down"
                )
                
                error.shouldBeInstanceOf<DomainServiceError.ServiceOperationError>()
                error.serviceName shouldBe "ValidationService"
                error.reason shouldBe "Service is temporarily down"
            }
            
            it("should provide context for operation timeout errors") {
                val error = DomainServiceError.ServiceOperationError.OperationTimeout(
                    operation = "validateScopeCreation",
                    timeoutMs = 5000L
                )
                
                error.operation shouldBe "validateScopeCreation"
                error.timeoutMs shouldBe 5000L
            }
            
            it("should provide context for configuration errors") {
                val error = DomainServiceError.ServiceOperationError.ConfigurationError(
                    configKey = "max_hierarchy_depth",
                    expectedType = "Integer",
                    actualValue = "unlimited"
                )
                
                error.configKey shouldBe "max_hierarchy_depth"
                error.expectedType shouldBe "Integer"
                error.actualValue shouldBe "unlimited"
            }
        }
        
        describe("RepositoryIntegrationError") {
            it("should provide context for repository operation failures") {
                val repositoryError = RepositoryError.ConnectionError(RuntimeException("Database unavailable"))
                val error = DomainServiceError.RepositoryIntegrationError.OperationFailed(
                    operation = "findHierarchyDepth",
                    repositoryError = repositoryError
                )
                
                error.shouldBeInstanceOf<DomainServiceError.RepositoryIntegrationError>()
                error.operation shouldBe "findHierarchyDepth"
                error.repositoryError shouldBe repositoryError
            }
            
            it("should provide context for data consistency errors") {
                val scopeId = ScopeId.generate()
                val error = DomainServiceError.RepositoryIntegrationError.DataConsistencyError(
                    scopeId = scopeId,
                    inconsistencyType = "parent_child_mismatch",
                    details = "Parent scope references non-existent child"
                )
                
                error.scopeId shouldBe scopeId
                error.inconsistencyType shouldBe "parent_child_mismatch"
                error.details shouldBe "Parent scope references non-existent child"
            }
        }
        
        describe("ExternalServiceError") {
            it("should provide context for external service integration failures") {
                val error = DomainServiceError.ExternalServiceError.IntegrationFailure(
                    serviceName = "TitleNormalizationService",
                    operation = "normalize",
                    errorCode = "NORM_001"
                )
                
                error.shouldBeInstanceOf<DomainServiceError.ExternalServiceError>()
                error.serviceName shouldBe "TitleNormalizationService"
                error.operation shouldBe "normalize"
                error.errorCode shouldBe "NORM_001"
            }
            
            it("should provide context for service authentication errors") {
                val error = DomainServiceError.ExternalServiceError.AuthenticationFailure(
                    serviceName = "ValidationAPIService",
                    reason = "API key expired"
                )
                
                error.serviceName shouldBe "ValidationAPIService"
                error.reason shouldBe "API key expired"
            }
        }
    }
})