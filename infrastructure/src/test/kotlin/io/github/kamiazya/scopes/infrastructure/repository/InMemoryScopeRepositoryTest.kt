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
    ): Scope {
        val scopeTitle = ScopeTitle.create(title).getOrNull()
            ?: error("Invalid title for test scope: '$title'")

        return Scope(
            id = id,
            title = scopeTitle,
            description = ScopeDescription.create(description).getOrNull(),
            parentId = parentId,
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now()
        )
    }

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

    "existsByParentIdAndTitle should perform case-insensitive comparison" {
        runTest {
            val repository = InMemoryScopeRepository()
            val parentId = ScopeId.generate()

            // Save a scope with mixed case title
            val originalScope = createTestScope(
                title = "Website Project",
                parentId = parentId
            )
            repository.save(originalScope)

            // Test case-insensitive variations
            val testCases = listOf(
                "website project",    // All lowercase
                "WEBSITE PROJECT",    // All uppercase
                "Website Project",    // Original case
                "wEbSiTe PrOjEcT"    // Mixed case
            )

            testCases.forEach { testTitle ->
                val existsResult = repository.existsByParentIdAndTitle(parentId, testTitle)
                val exists = existsResult.shouldBeRight()
                exists shouldBe true
            }
        }
    }

    "existsByParentIdAndTitle should handle whitespace normalization" {
        runTest {
            val repository = InMemoryScopeRepository()
            val parentId = ScopeId.generate()

            // Save a scope with clean title
            val originalScope = createTestScope(
                title = "Task Name",
                parentId = parentId
            )
            repository.save(originalScope)

            // Test whitespace variations
            val testCases = listOf(
                "task name",         // Normalized
                " task name ",       // Leading/trailing spaces
                "\ttask name\n",     // Tab and newline
                "  TASK NAME  ",     // Case and multiple spaces
                " Task Name "        // Original case with spaces
            )

            testCases.forEach { testTitle ->
                val existsResult = repository.existsByParentIdAndTitle(parentId, testTitle)
                val exists = existsResult.shouldBeRight()
                exists shouldBe true
            }
        }
    }

    "existsByParentIdAndTitle should distinguish different parent contexts" {
        runTest {
            val repository = InMemoryScopeRepository()
            val parentId1 = ScopeId.generate()
            val parentId2 = ScopeId.generate()

            // Save scope under parent1
            val scope1 = createTestScope(
                title = "Duplicate Title",
                parentId = parentId1
            )
            repository.save(scope1)

            // Check existence under parent1 - should exist
            val exists1Result = repository.existsByParentIdAndTitle(parentId1, "duplicate title")
            val exists1 = exists1Result.shouldBeRight()
            exists1 shouldBe true

            // Check existence under parent2 - should NOT exist
            val exists2Result = repository.existsByParentIdAndTitle(parentId2, "duplicate title")
            val exists2 = exists2Result.shouldBeRight()
            exists2 shouldBe false

            // Check existence at root level - should NOT exist
            val existsRootResult = repository.existsByParentIdAndTitle(null, "duplicate title")
            val existsRoot = existsRootResult.shouldBeRight()
            existsRoot shouldBe false
        }
    }

    "existsByParentIdAndTitle should not match titles stored with different whitespace" {
        runTest {
            val repository = InMemoryScopeRepository()
            val parentId = ScopeId.generate()

            // Save a scope with whitespace in the stored title
            val scopeWithSpaces = createTestScope(
                title = " Spaced Title ",
                parentId = parentId
            )
            repository.save(scopeWithSpaces)

            // All these variations should match due to normalization
            val shouldMatch = listOf(
                "spaced title",
                "SPACED TITLE",
                " SPACED TITLE ",
                "\tSpaced Title\n"
            )

            shouldMatch.forEach { testTitle ->
                val existsResult = repository.existsByParentIdAndTitle(parentId, testTitle)
                val exists = existsResult.shouldBeRight()
                exists shouldBe true
            }

            // These should NOT match
            val shouldNotMatch = listOf(
                "different title",
                "spaced",
                "title"
            )

            shouldNotMatch.forEach { testTitle ->
                val existsResult = repository.existsByParentIdAndTitle(parentId, testTitle)
                val exists = existsResult.shouldBeRight()
                exists shouldBe false
            }
        }
    }
})
