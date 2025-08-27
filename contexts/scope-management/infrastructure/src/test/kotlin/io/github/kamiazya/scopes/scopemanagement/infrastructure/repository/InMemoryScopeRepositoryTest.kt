package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class InMemoryScopeRepositoryTest :
    DescribeSpec({

        describe("InMemoryScopeRepository") {
            lateinit var repository: InMemoryScopeRepository
            lateinit var scope: Scope

            beforeEach {
                repository = InMemoryScopeRepository()
                scope = Scope.create("Test Scope", "Test Description", null).getOrNull()!!
            }

            describe("save") {
                it("should save a scope successfully") {
                    // When
                    val result = repository.save(scope)

                    // Then
                    result.shouldBeRight()
                    val savedScope = result.getOrNull()
                    savedScope.shouldNotBeNull()
                    savedScope.id shouldBe scope.id
                    savedScope.title shouldBe scope.title
                }

                it("should overwrite existing scope with same ID") {
                    // Given
                    repository.save(scope)
                    val updatedScope = scope.copy(title = scope.title.copy(value = "Updated Title"))

                    // When
                    val result = repository.save(updatedScope)

                    // Then
                    result.shouldBeRight()
                    val savedScope = result.getOrNull()
                    savedScope.shouldNotBeNull()
                    savedScope.title.value shouldBe "Updated Title"
                }
            }

            describe("existsById") {
                it("should return false when scope does not exist") {
                    // Given
                    val nonExistentId = ScopeId.generate()

                    // When
                    val result = repository.existsById(nonExistentId)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe false
                }

                it("should return true when scope exists") {
                    // Given
                    repository.save(scope)

                    // When
                    val result = repository.existsById(scope.id)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe true
                }
            }

            describe("existsByParentIdAndTitle") {
                it("should return false when no scope exists with parent and title") {
                    // When
                    val result = repository.existsByParentIdAndTitle(null, "Non-existent Title")

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe false
                }

                it("should return true when scope exists with matching parent and title") {
                    // Given
                    repository.save(scope)

                    // When
                    val result = repository.existsByParentIdAndTitle(scope.parentId, scope.title.value)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe true
                }

                it("should handle case-insensitive title matching") {
                    // Given
                    repository.save(scope)

                    // When
                    val result = repository.existsByParentIdAndTitle(scope.parentId, scope.title.value.uppercase())

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe true
                }

                it("should return false for invalid title") {
                    // When (empty string is invalid)
                    val result = repository.existsByParentIdAndTitle(null, "")

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe false
                }
            }

            describe("findIdByParentIdAndTitle") {
                it("should return null when no scope exists with parent and title") {
                    // When
                    val result = repository.findIdByParentIdAndTitle(null, "Non-existent Title")

                    // Then
                    result.shouldBeRight()
                    result.getOrNull().shouldBeNull()
                }

                it("should return scope ID when scope exists with matching parent and title") {
                    // Given
                    repository.save(scope)

                    // When
                    val result = repository.findIdByParentIdAndTitle(scope.parentId, scope.title.value)

                    // Then
                    result.shouldBeRight()
                    val foundId = result.getOrNull()
                    foundId.shouldNotBeNull()
                    foundId shouldBe scope.id
                }

                it("should handle case-insensitive title matching") {
                    // Given
                    repository.save(scope)

                    // When
                    val result = repository.findIdByParentIdAndTitle(scope.parentId, scope.title.value.lowercase())

                    // Then
                    result.shouldBeRight()
                    val foundId = result.getOrNull()
                    foundId.shouldNotBeNull()
                    foundId shouldBe scope.id
                }
            }

            describe("findByParentId") {
                it("should return empty list when no child scopes exist") {
                    // Given
                    val parentId = ScopeId.generate()

                    // When
                    val result = repository.findByParentId(parentId)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull()!!.shouldHaveSize(0)
                }

                it("should return root scopes when parentId is null") {
                    // Given
                    repository.save(scope) // root scope (parentId = null)

                    // When
                    val result = repository.findByParentId(null)

                    // Then
                    result.shouldBeRight()
                    val rootScopes = result.getOrNull()!!
                    rootScopes.shouldHaveSize(1)
                    rootScopes.shouldContain(scope)
                }

                it("should return child scopes for a specific parent") {
                    // Given
                    val parentScope = Scope.create("Parent Scope", null, null).getOrNull()!!
                    val childScope1 = Scope.create("Child 1", null, parentScope.id).getOrNull()!!
                    val childScope2 = Scope.create("Child 2", null, parentScope.id).getOrNull()!!

                    repository.save(parentScope)
                    repository.save(childScope1)
                    repository.save(childScope2)

                    // When
                    val result = repository.findByParentId(parentScope.id)

                    // Then
                    result.shouldBeRight()
                    val childScopes = result.getOrNull()!!
                    childScopes.shouldHaveSize(2)
                    childScopes.shouldContain(childScope1)
                    childScopes.shouldContain(childScope2)
                }
            }

            describe("deleteById") {
                it("should delete existing scope") {
                    // Given
                    repository.save(scope)

                    // When
                    val result = repository.deleteById(scope.id)

                    // Then
                    result.shouldBeRight()

                    // Verify deletion
                    val existsResult = repository.existsById(scope.id)
                    existsResult.getOrNull() shouldBe false
                }

                it("should handle deletion of non-existent scope gracefully") {
                    // Given
                    val nonExistentId = ScopeId.generate()

                    // When
                    val result = repository.deleteById(nonExistentId)

                    // Then
                    result.shouldBeRight()
                }
            }

            describe("findById") {
                it("should return null when scope does not exist") {
                    // Given
                    val nonExistentId = ScopeId.generate()

                    // When
                    val result = repository.findById(nonExistentId)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull().shouldBeNull()
                }

                it("should return scope when it exists") {
                    // Given
                    repository.save(scope)

                    // When
                    val result = repository.findById(scope.id)

                    // Then
                    result.shouldBeRight()
                    val foundScope = result.getOrNull()
                    foundScope.shouldNotBeNull()
                    foundScope shouldBe scope
                }
            }

            describe("findAll") {
                it("should return empty list when repository is empty") {
                    // When
                    val result = repository.findAll()

                    // Then
                    result.shouldBeRight()
                    result.getOrNull()!!.shouldHaveSize(0)
                }

                it("should return all scopes") {
                    // Given
                    val scope1 = Scope.create("Scope 1", null, null).getOrNull()!!
                    val scope2 = Scope.create("Scope 2", null, null).getOrNull()!!

                    repository.save(scope1)
                    repository.save(scope2)

                    // When
                    val result = repository.findAll()

                    // Then
                    result.shouldBeRight()
                    val allScopes = result.getOrNull()!!
                    allScopes.shouldHaveSize(2)
                    allScopes.shouldContain(scope1)
                    allScopes.shouldContain(scope2)
                }
            }

            describe("update") {
                it("should update existing scope") {
                    // Given
                    repository.save(scope)
                    val updatedScope = scope.copy(title = scope.title.copy(value = "Updated Title"))

                    // When
                    val result = repository.update(updatedScope)

                    // Then
                    result.shouldBeRight()
                    val savedScope = result.getOrNull()
                    savedScope.shouldNotBeNull()
                    savedScope.title.value shouldBe "Updated Title"

                    // Verify persistence
                    val findResult = repository.findById(scope.id)
                    findResult.getOrNull()!!.title.value shouldBe "Updated Title"
                }

                it("should create scope if it does not exist (in-memory behavior)") {
                    // Given
                    val newScope = Scope.create("New Scope", null, null).getOrNull()!!

                    // When
                    val result = repository.update(newScope)

                    // Then
                    result.shouldBeRight()
                    val savedScope = result.getOrNull()
                    savedScope.shouldNotBeNull()

                    // Verify persistence
                    val existsResult = repository.existsById(newScope.id)
                    existsResult.getOrNull() shouldBe true
                }
            }

            describe("utility methods") {
                describe("clear") {
                    it("should clear all scopes") {
                        // Given
                        repository.save(scope)

                        // When
                        repository.clear()

                        // Then
                        val sizeResult = repository.size()
                        sizeResult shouldBe 0
                    }
                }

                describe("size") {
                    it("should return correct count") {
                        // Given
                        val scope1 = Scope.create("Scope 1", null, null).getOrNull()!!
                        val scope2 = Scope.create("Scope 2", null, null).getOrNull()!!

                        repository.save(scope1)
                        repository.save(scope2)

                        // When
                        val size = repository.size()

                        // Then
                        size shouldBe 2
                    }
                }
            }
        }
    })
