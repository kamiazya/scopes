package io.github.kamiazya.scopes.infrastructure.repository

import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.valueobject.ScopeTitle
import io.github.kamiazya.scopes.domain.valueobject.ScopeDescription
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

class InMemoryScopeRepositoryTest : StringSpec({

    // Helper function to create test scope with valid value objects
    fun createTestScope(
        id: ScopeId = ScopeId.generate(),
        title: String = "Test Scope",
        description: String? = "Test Description",
        parentId: ScopeId? = null
    ): Scope = Scope(
        id = id,
        title = ScopeTitle.create(title).getOrNull()!!,
        description = ScopeDescription.create(description).getOrNull(),
        parentId = parentId,
        createdAt = kotlinx.datetime.Clock.System.now(),
        updatedAt = kotlinx.datetime.Clock.System.now()
    )

    "should save scope" {
        runTest {
            val repository = InMemoryScopeRepository()
            val scope = createTestScope()

            val saveResult = repository.save(scope)
            val savedScope = saveResult.shouldBeRight()
            savedScope shouldBe scope
        }
    }

    "should check if scope exists" {
        runTest {
            val repository = InMemoryScopeRepository()
            val scope = createTestScope()

            repository.save(scope)

            val existsResult = repository.existsById(scope.id)
            val exists = existsResult.shouldBeRight()
            exists shouldBe true

            val nonExistentId = ScopeId.generate()
            val notExistsResult = repository.existsById(nonExistentId)
            val notExists = notExistsResult.shouldBeRight()
            notExists shouldBe false
        }
    }

    "should check title uniqueness by parent" {
        runTest {
            val repository = InMemoryScopeRepository()
            val parentId = ScopeId.generate()
            val scope = createTestScope(title = "Test Scope", description = null, parentId = parentId)

            repository.save(scope)

            val existsResult = repository.existsByParentIdAndTitle(parentId, "Test Scope")
            val exists = existsResult.shouldBeRight()
            exists shouldBe true

            val notExistsResult = repository.existsByParentIdAndTitle(parentId, "Different Title")
            val notExists = notExistsResult.shouldBeRight()
            notExists shouldBe false
        }
    }

    "should count children by parent" {
        runTest {
            val repository = InMemoryScopeRepository()
            val parentId = ScopeId.generate()

            val child1 = createTestScope(title = "Child 1", description = null, parentId = parentId)
            val child2 = createTestScope(title = "Child 2", description = null, parentId = parentId)

            repository.save(child1)
            repository.save(child2)

            val countResult = repository.countByParentId(parentId)
            val count = countResult.shouldBeRight()
            count shouldBe 2
        }
    }

    "should find hierarchy depth" {
        runTest {
            val repository = InMemoryScopeRepository()
            val rootId = ScopeId.generate()
            val child1Id = ScopeId.generate()
            val child2Id = ScopeId.generate()

            val root = createTestScope(id = rootId, title = "Root", description = null, parentId = null)
            val child1 = createTestScope(id = child1Id, title = "Child 1", description = null, parentId = rootId)
            val child2 = createTestScope(id = child2Id, title = "Child 2", description = null, parentId = child1Id)

            repository.save(root)
            repository.save(child1)
            repository.save(child2)

            val depthResult = repository.findHierarchyDepth(child2Id)
            val depth = depthResult.shouldBeRight()
            depth shouldBe 3
        }
    }

    "should return incremented depth when scopeId does not exist" {
        runTest {
            val repository = InMemoryScopeRepository()
            val nonExistentId = ScopeId.generate()

            // Test with empty repository - should return 1 (0 + 1)
            val emptyDepthResult = repository.findHierarchyDepth(nonExistentId)
            val emptyDepth = emptyDepthResult.shouldBeRight()
            emptyDepth shouldBe 1

            // Add a root scope and test with non-existent child
            val rootId = ScopeId.generate()
            val root = createTestScope(id = rootId, title = "Root", description = null, parentId = null)
            repository.save(root)

            // Test with non-existent scope - should still return 1 (0 + 1)
            val nonExistentDepthResult = repository.findHierarchyDepth(nonExistentId)
            val nonExistentDepth = nonExistentDepthResult.shouldBeRight()
            nonExistentDepth shouldBe 1
        }
    }
})
