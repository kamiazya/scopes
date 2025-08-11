package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.error.BusinessRuleServiceError
import io.github.kamiazya.scopes.domain.error.ScopeBusinessRuleError
import io.github.kamiazya.scopes.domain.error.HierarchyBusinessRuleError
import io.github.kamiazya.scopes.domain.error.DataIntegrityBusinessRuleError
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Test class for BusinessRuleServiceError hierarchy.
 * 
 * Tests verify that business rule-specific error types provide appropriate
 * context for domain business rule violations.
 */
class BusinessRuleServiceErrorTest : DescribeSpec({

    describe("BusinessRuleServiceError hierarchy") {
        
        describe("ScopeBusinessRuleError") {
            it("should provide context for maximum depth exceeded errors") {
                val error = ScopeBusinessRuleError.MaxDepthExceeded(
                    maxDepth = 10,
                    actualDepth = 11,
                    scopeId = "test-scope-id",
                    parentPath = listOf("root", "parent")
                )
                
                error.shouldBeInstanceOf<ScopeBusinessRuleError>()
                error.maxDepth shouldBe 10
                error.actualDepth shouldBe 11
                error.scopeId shouldBe "test-scope-id"
                error.parentPath shouldBe listOf("root", "parent")
            }
            
            it("should provide context for maximum children exceeded errors") {
                val error = ScopeBusinessRuleError.MaxChildrenExceeded(
                    maxChildren = 100,
                    currentChildren = 100,
                    parentId = "parent-id",
                    attemptedOperation = "create child scope"
                )
                
                error.maxChildren shouldBe 100
                error.currentChildren shouldBe 100
                error.parentId shouldBe "parent-id"
                error.attemptedOperation shouldBe "create child scope"
            }
            
            it("should provide context for duplicate scope errors") {
                val error = ScopeBusinessRuleError.DuplicateScope(
                    duplicateTitle = "Duplicate Title",
                    parentId = "parent-id",
                    existingScopeId = "existing-scope-id",
                    normalizedTitle = "duplicate title"
                )
                
                error.duplicateTitle shouldBe "Duplicate Title"
                error.parentId shouldBe "parent-id"
                error.existingScopeId shouldBe "existing-scope-id"
                error.normalizedTitle shouldBe "duplicate title"
            }
        }
        
        describe("HierarchyBusinessRuleError") {
            it("should provide context for self-parenting errors") {
                val error = HierarchyBusinessRuleError.SelfParenting(
                    scopeId = "scope-id",
                    operation = "set parent"
                )
                
                error.shouldBeInstanceOf<HierarchyBusinessRuleError>()
                error.scopeId shouldBe "scope-id"
                error.operation shouldBe "set parent"
            }
            
            it("should provide context for circular reference errors") {
                val error = HierarchyBusinessRuleError.CircularReference(
                    scopeId = "scope-id",
                    parentId = "parent-id",
                    cyclePath = listOf("scope-id", "parent-id", "scope-id")
                )
                
                error.scopeId shouldBe "scope-id"
                error.parentId shouldBe "parent-id"
                error.cyclePath shouldBe listOf("scope-id", "parent-id", "scope-id")
            }
        }
        
        describe("DataIntegrityBusinessRuleError") {
            it("should provide context for consistency check failures") {
                val error = DataIntegrityBusinessRuleError.ConsistencyCheckFailure(
                    scopeId = "scope-id",
                    checkType = "hierarchy consistency",
                    expectedState = "valid hierarchy",
                    actualState = "invalid hierarchy",
                    affectedFields = listOf("parentId", "children")
                )
                
                error.shouldBeInstanceOf<DataIntegrityBusinessRuleError>()
                error.scopeId shouldBe "scope-id"
                error.checkType shouldBe "hierarchy consistency"
                error.expectedState shouldBe "valid hierarchy"
                error.actualState shouldBe "invalid hierarchy"
                error.affectedFields shouldBe listOf("parentId", "children")
            }
        }
    }
})