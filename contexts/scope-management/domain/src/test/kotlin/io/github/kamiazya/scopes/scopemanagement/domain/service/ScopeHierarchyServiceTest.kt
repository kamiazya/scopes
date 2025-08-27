package io.github.kamiazya.scopes.scopemanagement.domain.service

import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.kotest.assertions.fail
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ScopeHierarchyServiceTest :
    DescribeSpec({
        val service = ScopeHierarchyService()

        describe("calculateHierarchyDepth") {
            it("should calculate depth for root scope") {
                val rootId = ScopeId.generate()
                val rootScope = Scope.create("Root Scope", null, null).getOrNull()!!.copy(id = rootId)

                val getScopeById: suspend (ScopeId) -> Scope? = { id ->
                    when (id) {
                        rootId -> rootScope
                        else -> null
                    }
                }

                val result = service.calculateHierarchyDepth(rootId, getScopeById)

                result.isRight() shouldBe true
                result.fold(
                    { fail("Expected success but got error: $it") },
                    { depth -> depth shouldBe 1 },
                )
            }

            it("should calculate depth for nested hierarchy") {
                val rootId = ScopeId.generate()
                val childId = ScopeId.generate()
                val grandchildId = ScopeId.generate()

                val rootScope = Scope.create("Root", null, null).getOrNull()!!.copy(id = rootId)
                val childScope = Scope.create("Child", null, rootId).getOrNull()!!.copy(id = childId)
                val grandchildScope = Scope.create("Grandchild", null, childId).getOrNull()!!.copy(id = grandchildId)

                val getScopeById: suspend (ScopeId) -> Scope? = { id ->
                    when (id) {
                        rootId -> rootScope
                        childId -> childScope
                        grandchildId -> grandchildScope
                        else -> null
                    }
                }

                val result = service.calculateHierarchyDepth(grandchildId, getScopeById)

                result.isRight() shouldBe true
                result.fold(
                    { fail("Expected success but got error: $it") },
                    { depth -> depth shouldBe 3 },
                )
            }

            it("should detect circular reference") {
                val scope1Id = ScopeId.generate()
                val scope2Id = ScopeId.generate()
                val scope3Id = ScopeId.generate()

                // Create circular reference: 1 -> 2 -> 3 -> 1
                val scope1 = Scope.create("Scope1", null, scope3Id).getOrNull()!!.copy(id = scope1Id)
                val scope2 = Scope.create("Scope2", null, scope1Id).getOrNull()!!.copy(id = scope2Id)
                val scope3 = Scope.create("Scope3", null, scope2Id).getOrNull()!!.copy(id = scope3Id)

                val getScopeById: suspend (ScopeId) -> Scope? = { id ->
                    when (id) {
                        scope1Id -> scope1
                        scope2Id -> scope2
                        scope3Id -> scope3
                        else -> null
                    }
                }

                val result = service.calculateHierarchyDepth(scope1Id, getScopeById)

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        error.shouldBeInstanceOf<ScopeHierarchyError.CircularPath>()
                    },
                    { fail("Expected error but got success: $it") },
                )
            }

            it("should fail when scope not found") {
                val missingId = ScopeId.generate()
                val getScopeById: suspend (ScopeId) -> Scope? = { _ -> null }

                val result = service.calculateHierarchyDepth(missingId, getScopeById)

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        error.shouldBeInstanceOf<ScopeHierarchyError.ScopeInHierarchyNotFound>()
                        error.scopeId shouldBe missingId
                    },
                    { fail("Expected error but got success: $it") },
                )
            }
        }

        describe("validateParentChildRelationship") {
            it("should allow valid parent-child relationship") {
                val parentId = ScopeId.generate()
                val childId = ScopeId.generate()

                val parentScope = Scope.create("Parent", null, null).getOrNull()!!.copy(id = parentId)
                val childScope = Scope.create("Child", null, null).getOrNull()!!.copy(id = childId)

                val getScopeById: suspend (ScopeId) -> Scope? = { _ -> null }

                val result = service.validateParentChildRelationship(parentScope, childScope, getScopeById)

                result.isRight() shouldBe true
            }

            it("should prevent self-parenting") {
                val scopeId = ScopeId.generate()
                val scope = Scope.create("Scope", null, null).getOrNull()!!.copy(id = scopeId)

                val getScopeById: suspend (ScopeId) -> Scope? = { _ -> null }

                val result = service.validateParentChildRelationship(scope, scope, getScopeById)

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        error.shouldBeInstanceOf<ScopeHierarchyError.SelfParenting>()
                        error.scopeId shouldBe scopeId
                    },
                    { fail("Expected error but got success: $it") },
                )
            }

            it("should detect circular reference when parent is descendant of child") {
                val grandparentId = ScopeId.generate()
                val parentId = ScopeId.generate()
                val childId = ScopeId.generate()

                // Current hierarchy: grandparent -> parent -> child
                // Trying to make: child -> grandparent (would create cycle)
                val grandparentScope = Scope.create("Grandparent", null, null).getOrNull()!!.copy(id = grandparentId)
                val parentScope = Scope.create("Parent", null, grandparentId).getOrNull()!!.copy(id = parentId, parentId = childId)
                val childScope = Scope.create("Child", null, parentId).getOrNull()!!.copy(id = childId)

                val getScopeById: suspend (ScopeId) -> Scope? = { id ->
                    when (id) {
                        childId -> childScope
                        else -> null
                    }
                }

                val result = service.validateParentChildRelationship(parentScope, childScope, getScopeById)

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        error.shouldBeInstanceOf<ScopeHierarchyError.CircularReference>()
                        error.scopeId shouldBe childId
                        error.parentId shouldBe parentId
                    },
                    { fail("Expected error but got success: $it") },
                )
            }
        }

        describe("validateChildrenLimit") {
            it("should allow adding child when under limit") {
                val parentId = ScopeId.generate()

                val result = service.validateChildrenLimit(parentId, currentChildCount = 2, maxChildren = 5)

                result.isRight() shouldBe true
            }

            it("should allow unlimited children when maxChildren is null") {
                val parentId = ScopeId.generate()

                val result = service.validateChildrenLimit(parentId, currentChildCount = 1000, maxChildren = null)

                result.isRight() shouldBe true
            }

            it("should prevent exceeding children limit") {
                val parentId = ScopeId.generate()

                val result = service.validateChildrenLimit(parentId, currentChildCount = 5, maxChildren = 5)

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        error.shouldBeInstanceOf<ScopeHierarchyError.MaxChildrenExceeded>()
                        error.parentScopeId shouldBe parentId
                        error.currentChildrenCount shouldBe 5
                        error.maximumChildren shouldBe 5
                    },
                    { fail("Expected error but got success: $it") },
                )
            }
        }

        describe("validateHierarchyDepth") {
            it("should allow depth within limit") {
                val scopeId = ScopeId.generate()

                val result = service.validateHierarchyDepth(scopeId, currentDepth = 3, maxDepth = 10)

                result.isRight() shouldBe true
            }

            it("should allow unlimited depth when maxDepth is null") {
                val scopeId = ScopeId.generate()

                val result = service.validateHierarchyDepth(scopeId, currentDepth = 100, maxDepth = null)

                result.isRight() shouldBe true
            }

            it("should prevent exceeding depth limit") {
                val scopeId = ScopeId.generate()

                val result = service.validateHierarchyDepth(scopeId, currentDepth = 5, maxDepth = 5)

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        error.shouldBeInstanceOf<ScopeHierarchyError.MaxDepthExceeded>()
                        error.scopeId shouldBe scopeId
                        error.attemptedDepth shouldBe 6
                        error.maximumDepth shouldBe 5
                    },
                    { fail("Expected error but got success: $it") },
                )
            }

            it("should allow depth exactly at limit") {
                val scopeId = ScopeId.generate()

                val result = service.validateHierarchyDepth(scopeId, currentDepth = 4, maxDepth = 5)

                result.isRight() shouldBe true
            }
        }
    })
