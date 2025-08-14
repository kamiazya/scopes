package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.error.BusinessRuleServiceError
import io.github.kamiazya.scopes.domain.error.ScopeBusinessRuleError
import io.github.kamiazya.scopes.domain.error.HierarchyBusinessRuleError
import io.github.kamiazya.scopes.domain.error.DataIntegrityBusinessRuleError
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
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
                val scopeId = ScopeId.generate()
                val rootId = ScopeId.generate()
                val parentId = ScopeId.generate()
                val error = ScopeBusinessRuleError.MaxDepthExceeded(
                    maxDepth = 10,
                    actualDepth = 11,
                    scopeId = scopeId,
                    parentPath = listOf(rootId, parentId)
                )
                
                error.shouldBeInstanceOf<ScopeBusinessRuleError>()
                error.maxDepth shouldBe 10
                error.actualDepth shouldBe 11
                error.scopeId shouldBe scopeId
                error.parentPath shouldBe listOf(rootId, parentId)
            }
            
            it("should provide context for maximum children exceeded errors") {
                val parentId = ScopeId.generate()
                val error = ScopeBusinessRuleError.MaxChildrenExceeded(
                    maxChildren = 100,
                    currentChildren = 100,
                    parentId = parentId,
                    attemptedOperation = "create child scope"
                )
                
                error.maxChildren shouldBe 100
                error.currentChildren shouldBe 100
                error.parentId shouldBe parentId
                error.attemptedOperation shouldBe "create child scope"
            }
            
            it("should provide context for duplicate scope errors") {
                val parentId = ScopeId.generate()
                val existingScopeId = ScopeId.generate()
                val error = ScopeBusinessRuleError.DuplicateScope(
                    duplicateTitle = "Duplicate Title",
                    parentId = parentId,
                    existingScopeId = existingScopeId,
                    normalizedTitle = "duplicate title"
                )
                
                error.duplicateTitle shouldBe "Duplicate Title"
                error.parentId shouldBe parentId
                error.existingScopeId shouldBe existingScopeId
                error.normalizedTitle shouldBe "duplicate title"
            }
        }
        
        describe("HierarchyBusinessRuleError") {
            it("should provide context for self-parenting errors") {
                val scopeId = ScopeId.generate()
                val error = HierarchyBusinessRuleError.SelfParenting(
                    scopeId = scopeId,
                    operation = "set parent"
                )
                
                error.shouldBeInstanceOf<HierarchyBusinessRuleError>()
                error.scopeId shouldBe scopeId
                error.operation shouldBe "set parent"
            }
            
            it("should provide context for circular reference errors") {
                val scopeId = ScopeId.generate()
                val parentId = ScopeId.generate()
                val error = HierarchyBusinessRuleError.CircularReference(
                    scopeId = scopeId,
                    parentId = parentId,
                    cyclePath = listOf(scopeId, parentId, scopeId)
                )
                
                error.scopeId shouldBe scopeId
                error.parentId shouldBe parentId
                error.cyclePath shouldBe listOf(scopeId, parentId, scopeId)
            }
        }
        
        describe("DataIntegrityBusinessRuleError") {
            it("should provide context for consistency check failures") {
                val scopeId = ScopeId.generate()
                val error = DataIntegrityBusinessRuleError.ConsistencyCheckFailure(
                    scopeId = scopeId,
                    checkType = "hierarchy consistency",
                    expectedState = "valid hierarchy",
                    actualState = "invalid hierarchy",
                    affectedFields = listOf("parentId", "children")
                )
                
                error.shouldBeInstanceOf<DataIntegrityBusinessRuleError>()
                error.scopeId shouldBe scopeId
                error.checkType shouldBe "hierarchy consistency"
                error.expectedState shouldBe "valid hierarchy"
                error.actualState shouldBe "invalid hierarchy"
                error.affectedFields shouldBe listOf("parentId", "children")
            }
        }
    }
})