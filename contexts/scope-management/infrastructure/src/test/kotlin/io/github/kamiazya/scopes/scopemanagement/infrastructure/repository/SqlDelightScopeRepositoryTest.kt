package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Aspects
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
import io.github.kamiazya.scopes.scopemanagement.infrastructure.sqldelight.SqlDelightDatabaseProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock

class SqlDelightScopeRepositoryTest :
    DescribeSpec({

        describe("SqlDelightScopeRepository") {
            lateinit var repository: SqlDelightScopeRepository
            lateinit var database: AutoCloseable

            beforeEach {
                val db = SqlDelightDatabaseProvider.createInMemoryDatabase()
                database = db as AutoCloseable
                repository = SqlDelightScopeRepository(db)
            }

            afterEach {
                database.close()
            }

            describe("save") {
                it("should save a new scope") {
                    // Given
                    val scope = Scope(
                        id = ScopeId.generate(),
                        title = ScopeTitle.create("Test Scope").getOrNull()!!,
                        description = ScopeDescription.create("Test Description").getOrNull(),
                        parentId = null,
                        aspects = Aspects.empty(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )

                    // When
                    val result = runTest { repository.save(scope) }

                    // Then
                    result shouldBe scope.right()
                }

                it("should save a scope with aspects") {
                    // Given
                    val aspectKey = AspectKey.create("priority").getOrNull()!!
                    val aspectValue = AspectValue.create("high").getOrNull()!!
                    val aspects = Aspects.from(mapOf(aspectKey to listOf(aspectValue)))

                    val scope = Scope(
                        id = ScopeId.generate(),
                        title = ScopeTitle.create("Scope with Aspects").getOrNull()!!,
                        description = null,
                        parentId = null,
                        aspects = aspects,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )

                    // When
                    val saveResult = runTest { repository.save(scope) }
                    val findResult = runTest { repository.findById(scope.id) }

                    // Then
                    saveResult shouldBe scope.right()
                    findResult.isRight() shouldBe true
                    val foundScope = findResult.getOrNull()
                    foundScope shouldNotBe null
                    foundScope?.aspects?.toMap()?.get(aspectKey) shouldBe listOf(aspectValue)
                }

                it("should update an existing scope") {
                    // Given
                    val scope = Scope(
                        id = ScopeId.generate(),
                        title = ScopeTitle.create("Initial Title").getOrNull()!!,
                        description = null,
                        parentId = null,
                        aspects = Aspects.empty(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )

                    runTest { repository.save(scope) }

                    val updatedScope = scope.copy(
                        title = ScopeTitle.create("Updated Title").getOrNull()!!,
                        description = ScopeDescription.create("New Description").getOrNull(),
                        updatedAt = Clock.System.now(),
                    )

                    // When
                    val result = runTest { repository.save(updatedScope) }
                    val findResult = runTest { repository.findById(scope.id) }

                    // Then
                    result shouldBe updatedScope.right()
                    findResult.isRight() shouldBe true
                    val foundScope = findResult.getOrNull()
                    foundScope?.title shouldBe updatedScope.title
                    foundScope?.description shouldBe updatedScope.description
                }

                it("should handle save errors gracefully") {
                    // Given
                    val scope = Scope(
                        id = ScopeId.generate(),
                        title = ScopeTitle.create("Test").getOrNull()!!,
                        description = null,
                        parentId = null,
                        aspects = Aspects.empty(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )

                    // Close database to simulate error
                    database.close()

                    // When
                    val result = runTest { repository.save(scope) }

                    // Then
                    result.isLeft() shouldBe true
                    result.leftOrNull().shouldBeInstanceOf<PersistenceError.StorageUnavailable>()
                }
            }

            describe("findById") {
                it("should find an existing scope by ID") {
                    // Given
                    val scope = Scope(
                        id = ScopeId.generate(),
                        title = ScopeTitle.create("Find Me").getOrNull()!!,
                        description = ScopeDescription.create("Test Description").getOrNull(),
                        parentId = null,
                        aspects = Aspects.empty(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )
                    runTest { repository.save(scope) }

                    // When
                    val result = runTest { repository.findById(scope.id) }

                    // Then
                    result.isRight() shouldBe true
                    val foundScope = result.getOrNull()
                    foundScope shouldNotBe null
                    foundScope?.id shouldBe scope.id
                    foundScope?.title shouldBe scope.title
                    foundScope?.description shouldBe scope.description
                }

                it("should return null for non-existent scope") {
                    // Given
                    val nonExistentId = ScopeId.generate()

                    // When
                    val result = runTest { repository.findById(nonExistentId) }

                    // Then
                    result shouldBe null.right()
                }

                it("should handle findById errors gracefully") {
                    // Given
                    val scopeId = ScopeId.generate()
                    database.close()

                    // When
                    val result = runTest { repository.findById(scopeId) }

                    // Then
                    result.isLeft() shouldBe true
                    result.leftOrNull().shouldBeInstanceOf<PersistenceError.StorageUnavailable>()
                }
            }

            describe("findAll") {
                it("should return all scopes") {
                    // Given
                    val scopes = listOf(
                        Scope(
                            id = ScopeId.generate(),
                            title = ScopeTitle.create("Scope 1").getOrNull()!!,
                            description = null,
                            parentId = null,
                            aspects = Aspects.empty(),
                            createdAt = Clock.System.now(),
                            updatedAt = Clock.System.now(),
                        ),
                        Scope(
                            id = ScopeId.generate(),
                            title = ScopeTitle.create("Scope 2").getOrNull()!!,
                            description = null,
                            parentId = null,
                            aspects = Aspects.empty(),
                            createdAt = Clock.System.now(),
                            updatedAt = Clock.System.now(),
                        ),
                    )

                    scopes.forEach { scope ->
                        runTest { repository.save(scope) }
                    }

                    // When
                    val result = runTest { repository.findAll() }

                    // Then
                    result.isRight() shouldBe true
                    val foundScopes = result.getOrNull()
                    foundScopes?.size shouldBe 2
                    foundScopes?.map { it.title } shouldBe scopes.map { it.title }
                }

                it("should return empty list when no scopes exist") {
                    // When
                    val result = runTest { repository.findAll() }

                    // Then
                    result shouldBe emptyList<Scope>().right()
                }
            }

            describe("findByParentId") {
                it("should find root scopes when parentId is null") {
                    // Given
                    val rootScope = Scope(
                        id = ScopeId.generate(),
                        title = ScopeTitle.create("Root Scope").getOrNull()!!,
                        description = null,
                        parentId = null,
                        aspects = Aspects.empty(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )

                    val childScope = Scope(
                        id = ScopeId.generate(),
                        title = ScopeTitle.create("Child Scope").getOrNull()!!,
                        description = null,
                        parentId = ScopeId.generate(),
                        aspects = Aspects.empty(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )

                    runTest {
                        repository.save(rootScope)
                        repository.save(childScope)
                    }

                    // When
                    val result = runTest { repository.findByParentId(null) }

                    // Then
                    result.isRight() shouldBe true
                    val foundScopes = result.getOrNull()
                    foundScopes?.size shouldBe 1
                    foundScopes?.first()?.id shouldBe rootScope.id
                }

                it("should find child scopes by parent ID") {
                    // Given
                    val parentId = ScopeId.generate()
                    val parentScope = Scope(
                        id = parentId,
                        title = ScopeTitle.create("Parent").getOrNull()!!,
                        description = null,
                        parentId = null,
                        aspects = Aspects.empty(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )

                    val childScopes = listOf(
                        Scope(
                            id = ScopeId.generate(),
                            title = ScopeTitle.create("Child 1").getOrNull()!!,
                            description = null,
                            parentId = parentId,
                            aspects = Aspects.empty(),
                            createdAt = Clock.System.now(),
                            updatedAt = Clock.System.now(),
                        ),
                        Scope(
                            id = ScopeId.generate(),
                            title = ScopeTitle.create("Child 2").getOrNull()!!,
                            description = null,
                            parentId = parentId,
                            aspects = Aspects.empty(),
                            createdAt = Clock.System.now(),
                            updatedAt = Clock.System.now(),
                        ),
                    )

                    runTest {
                        repository.save(parentScope)
                        childScopes.forEach { repository.save(it) }
                    }

                    // When
                    val result = runTest { repository.findByParentId(parentId) }

                    // Then
                    result.isRight() shouldBe true
                    val foundScopes = result.getOrNull()
                    foundScopes?.size shouldBe 2
                    foundScopes?.all { it.parentId == parentId } shouldBe true
                }
            }

            describe("existsById") {
                it("should return true for existing scope") {
                    // Given
                    val scope = Scope(
                        id = ScopeId.generate(),
                        title = ScopeTitle.create("Exists").getOrNull()!!,
                        description = null,
                        parentId = null,
                        aspects = Aspects.empty(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )
                    runTest { repository.save(scope) }

                    // When
                    val result = runTest { repository.existsById(scope.id) }

                    // Then
                    result shouldBe true.right()
                }

                it("should return false for non-existent scope") {
                    // Given
                    val nonExistentId = ScopeId.generate()

                    // When
                    val result = runTest { repository.existsById(nonExistentId) }

                    // Then
                    result shouldBe false.right()
                }
            }

            describe("existsByParentIdAndTitle") {
                it("should return true when scope exists with given parent and title") {
                    // Given
                    val parentId = ScopeId.generate()
                    val title = "Unique Title"
                    val scope = Scope(
                        id = ScopeId.generate(),
                        title = ScopeTitle.create(title).getOrNull()!!,
                        description = null,
                        parentId = parentId,
                        aspects = Aspects.empty(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )
                    runTest { repository.save(scope) }

                    // When
                    val result = runTest { repository.existsByParentIdAndTitle(parentId, title) }

                    // Then
                    result shouldBe true.right()
                }

                it("should return false when scope doesn't exist with given parent and title") {
                    // When
                    val result = runTest { repository.existsByParentIdAndTitle(ScopeId.generate(), "Non-existent") }

                    // Then
                    result shouldBe false.right()
                }

                it("should check root scopes when parentId is null") {
                    // Given
                    val title = "Root Title"
                    val scope = Scope(
                        id = ScopeId.generate(),
                        title = ScopeTitle.create(title).getOrNull()!!,
                        description = null,
                        parentId = null,
                        aspects = Aspects.empty(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )
                    runTest { repository.save(scope) }

                    // When
                    val result = runTest { repository.existsByParentIdAndTitle(null, title) }

                    // Then
                    result shouldBe true.right()
                }
            }

            describe("deleteById") {
                it("should delete an existing scope and its aspects") {
                    // Given
                    val aspectKey = AspectKey.create("status").getOrNull()!!
                    val aspectValue = AspectValue.create("active").getOrNull()!!
                    val aspects = Aspects.from(mapOf(aspectKey to listOf(aspectValue)))

                    val scope = Scope(
                        id = ScopeId.generate(),
                        title = ScopeTitle.create("To Delete").getOrNull()!!,
                        description = null,
                        parentId = null,
                        aspects = aspects,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )
                    runTest { repository.save(scope) }

                    // When
                    val deleteResult = runTest { repository.deleteById(scope.id) }
                    val findResult = runTest { repository.findById(scope.id) }

                    // Then
                    deleteResult shouldBe Unit.right()
                    findResult shouldBe null.right()
                }

                it("should handle deletion of non-existent scope gracefully") {
                    // Given
                    val nonExistentId = ScopeId.generate()

                    // When
                    val result = runTest { repository.deleteById(nonExistentId) }

                    // Then
                    result shouldBe Unit.right()
                }
            }

            describe("countChildrenOf") {
                it("should count direct children of a scope") {
                    // Given
                    val parentId = ScopeId.generate()
                    val parentScope = Scope(
                        id = parentId,
                        title = ScopeTitle.create("Parent").getOrNull()!!,
                        description = null,
                        parentId = null,
                        aspects = Aspects.empty(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )

                    runTest { repository.save(parentScope) }

                    // Create 3 direct children
                    repeat(3) { i ->
                        val child = Scope(
                            id = ScopeId.generate(),
                            title = ScopeTitle.create("Child $i").getOrNull()!!,
                            description = null,
                            parentId = parentId,
                            aspects = Aspects.empty(),
                            createdAt = Clock.System.now(),
                            updatedAt = Clock.System.now(),
                        )
                        runTest { repository.save(child) }
                    }

                    // When
                    val result = runTest { repository.countChildrenOf(parentId) }

                    // Then
                    result shouldBe 3.right()
                }

                it("should return 0 for scope with no children") {
                    // Given
                    val scopeId = ScopeId.generate()

                    // When
                    val result = runTest { repository.countChildrenOf(scopeId) }

                    // Then
                    result shouldBe 0.right()
                }
            }

            describe("findDescendantsOf") {
                it("should find all descendants recursively") {
                    // Given
                    val rootId = ScopeId.generate()
                    val rootScope = Scope(
                        id = rootId,
                        title = ScopeTitle.create("Root").getOrNull()!!,
                        description = null,
                        parentId = null,
                        aspects = Aspects.empty(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )

                    val child1Id = ScopeId.generate()
                    val child1 = Scope(
                        id = child1Id,
                        title = ScopeTitle.create("Child 1").getOrNull()!!,
                        description = null,
                        parentId = rootId,
                        aspects = Aspects.empty(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )

                    val grandchild1 = Scope(
                        id = ScopeId.generate(),
                        title = ScopeTitle.create("Grandchild 1").getOrNull()!!,
                        description = null,
                        parentId = child1Id,
                        aspects = Aspects.empty(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )

                    val child2 = Scope(
                        id = ScopeId.generate(),
                        title = ScopeTitle.create("Child 2").getOrNull()!!,
                        description = null,
                        parentId = rootId,
                        aspects = Aspects.empty(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )

                    runTest {
                        repository.save(rootScope)
                        repository.save(child1)
                        repository.save(child2)
                        repository.save(grandchild1)
                    }

                    // When
                    val result = runTest { repository.findDescendantsOf(rootId) }

                    // Then
                    result.isRight() shouldBe true
                    val descendants = result.getOrNull()
                    descendants?.size shouldBe 3
                    descendants?.map { it.title.value }?.toSet() shouldBe setOf("Child 1", "Child 2", "Grandchild 1")
                }

                it("should return empty list for scope with no descendants") {
                    // Given
                    val scopeId = ScopeId.generate()
                    val scope = Scope(
                        id = scopeId,
                        title = ScopeTitle.create("Leaf").getOrNull()!!,
                        description = null,
                        parentId = null,
                        aspects = Aspects.empty(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )
                    runTest { repository.save(scope) }

                    // When
                    val result = runTest { repository.findDescendantsOf(scopeId) }

                    // Then
                    result shouldBe emptyList<Scope>().right()
                }
            }

            describe("findIdByParentIdAndTitle") {
                it("should find scope ID by parent and title") {
                    // Given
                    val parentId = ScopeId.generate()
                    val title = "Specific Title"
                    val scope = Scope(
                        id = ScopeId.generate(),
                        title = ScopeTitle.create(title).getOrNull()!!,
                        description = null,
                        parentId = parentId,
                        aspects = Aspects.empty(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )
                    runTest { repository.save(scope) }

                    // When
                    val result = runTest { repository.findIdByParentIdAndTitle(parentId, title) }

                    // Then
                    result shouldBe scope.id.right()
                }

                it("should return null when scope not found") {
                    // When
                    val result = runTest { repository.findIdByParentIdAndTitle(ScopeId.generate(), "Not Found") }

                    // Then
                    result shouldBe null.right()
                }

                it("should find root scope ID when parentId is null") {
                    // Given
                    val title = "Root Scope Title"
                    val scope = Scope(
                        id = ScopeId.generate(),
                        title = ScopeTitle.create(title).getOrNull()!!,
                        description = null,
                        parentId = null,
                        aspects = Aspects.empty(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )
                    runTest { repository.save(scope) }

                    // When
                    val result = runTest { repository.findIdByParentIdAndTitle(null, title) }

                    // Then
                    result shouldBe scope.id.right()
                }
            }

            describe("error handling") {
                it("should handle database errors consistently across all operations") {
                    // Given
                    database.close()

                    // When/Then - Test all operations return proper errors
                    val operations = listOf(
                        runTest { repository.save(Scope(ScopeId.generate(), ScopeTitle.create("Test").getOrNull()!!, null, null, Aspects.empty(), Clock.System.now(), Clock.System.now())) },
                        runTest { repository.findById(ScopeId.generate()) },
                        runTest { repository.findAll() },
                        runTest { repository.findByParentId(null) },
                        runTest { repository.existsById(ScopeId.generate()) },
                        runTest { repository.existsByParentIdAndTitle(null, "Test") },
                        runTest { repository.deleteById(ScopeId.generate()) },
                        runTest { repository.countChildrenOf(ScopeId.generate()) },
                        runTest { repository.findDescendantsOf(ScopeId.generate()) },
                        runTest { repository.findIdByParentIdAndTitle(null, "Test") },
                    )

                    operations.forEach { result ->
                        result.isLeft() shouldBe true
                        val error = result.leftOrNull()
                        error.shouldBeInstanceOf<PersistenceError.StorageUnavailable>()
                        error.operation shouldNotBe null
                    }
                }
            }
        }
    })