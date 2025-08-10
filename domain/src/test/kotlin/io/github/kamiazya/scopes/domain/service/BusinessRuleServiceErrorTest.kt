package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.error.BusinessRuleServiceError
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
                val error = BusinessRuleServiceError.ScopeBusinessRuleError.MaxDepthExceeded(
                    maxDepth = 10,
                    attemptedDepth = 11,
                    affectedScopeId = ScopeId.generate()
                )
                
                error.shouldBeInstanceOf<BusinessRuleServiceError.ScopeBusinessRuleError>()
                error.maxDepth shouldBe 10
                error.attemptedDepth shouldBe 11
            }
            
            it("should provide context for maximum children exceeded errors") {
                val parentId = ScopeId.generate()
                val error = BusinessRuleServiceError.ScopeBusinessRuleError.MaxChildrenExceeded(
                    maxChildren = 100,
                    currentChildren = 100,
                    parentId = parentId
                )
                
                error.maxChildren shouldBe 100
                error.currentChildren shouldBe 100
                error.parentId shouldBe parentId
            }
            
            it("should provide context for duplicate title errors with business context") {
                val parentId = ScopeId.generate()
                val error = BusinessRuleServiceError.ScopeBusinessRuleError.DuplicateTitleNotAllowed(
                    title = "Duplicate Title",
                    parentId = parentId,
                    conflictContext = "Same parent scope"
                )
                
                error.title shouldBe "Duplicate Title"
                error.parentId shouldBe parentId
                error.conflictContext shouldBe "Same parent scope"
            }
        }
        
        describe("HierarchyBusinessRuleError") {
            it("should provide context for self-parenting errors") {
                val scopeId = ScopeId.generate()
                val error = BusinessRuleServiceError.HierarchyBusinessRuleError.SelfParentingNotAllowed(
                    scopeId = scopeId
                )
                
                error.shouldBeInstanceOf<BusinessRuleServiceError.HierarchyBusinessRuleError>()
                error.scopeId shouldBe scopeId
            }
            
            it("should provide context for circular reference business rule errors") {
                val scopeId = ScopeId.generate()
                val parentId = ScopeId.generate()
                val error = BusinessRuleServiceError.HierarchyBusinessRuleError.CircularReferenceNotAllowed(
                    scopeId = scopeId,
                    parentId = parentId,
                    circularPath = listOf(scopeId, parentId, scopeId)
                )
                
                error.scopeId shouldBe scopeId
                error.parentId shouldBe parentId
                error.circularPath shouldBe listOf(scopeId, parentId, scopeId)
            }
            
            it("should provide context for orphaned scope creation prevention") {
                val parentId = ScopeId.generate()
                val error = BusinessRuleServiceError.HierarchyBusinessRuleError.OrphanedScopeCreationNotAllowed(
                    parentId = parentId,
                    reason = "Parent scope does not exist"
                )
                
                error.parentId shouldBe parentId
                error.reason shouldBe "Parent scope does not exist"
            }
        }
        
        describe("DataIntegrityBusinessRuleError") {
            it("should provide context for consistency check failures") {
                val scopeId = ScopeId.generate()
                val error = BusinessRuleServiceError.DataIntegrityBusinessRuleError.ConsistencyCheckFailed(
                    scopeId = scopeId,
                    failedChecks = listOf("parent_child_consistency", "hierarchy_depth_consistency")
                )
                
                error.shouldBeInstanceOf<BusinessRuleServiceError.DataIntegrityBusinessRuleError>()
                error.scopeId shouldBe scopeId
                error.failedChecks shouldBe listOf("parent_child_consistency", "hierarchy_depth_consistency")
            }
            
            it("should provide context for referential integrity errors") {
                val scopeId = ScopeId.generate()
                val referencedId = ScopeId.generate()
                val error = BusinessRuleServiceError.DataIntegrityBusinessRuleError.ReferentialIntegrityViolation(
                    scopeId = scopeId,
                    referencedId = referencedId,
                    referenceType = "parent_id"
                )
                
                error.scopeId shouldBe scopeId
                error.referencedId shouldBe referencedId
                error.referenceType shouldBe "parent_id"
            }
        }
    }
})