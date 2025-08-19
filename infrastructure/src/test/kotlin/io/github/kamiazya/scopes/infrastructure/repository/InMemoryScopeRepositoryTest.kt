package io.github.kamiazya.scopes.infrastructure.repository

import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.valueobject.ScopeTitle
import io.github.kamiazya.scopes.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.domain.error.PersistenceError
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.arrow.core.shouldBeLeft
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

            val childrenResult = repository.findByParentId(parentId)
            val children = childrenResult.shouldBeRight()
            children.size shouldBe 2
        }
    }

    "should find children by parent ID" {
        runTest {
            val repository = InMemoryScopeRepository()
            val parentId = ScopeId.generate()
            val child1Id = ScopeId.generate()
            val child2Id = ScopeId.generate()

            val parent = createTestScope(id = parentId, title = "Parent", description = null, parentId = null)
            val child1 = createTestScope(id = child1Id, title = "Child 1", description = null, parentId = parentId)
            val child2 = createTestScope(id = child2Id, title = "Child 2", description = null, parentId = parentId)

            repository.save(parent)
            repository.save(child1)
            repository.save(child2)

            val childrenResult = repository.findByParentId(parentId)
            val children = childrenResult.shouldBeRight()
            children.size shouldBe 2
            children.map { it.id }.toSet() shouldBe setOf(child1Id, child2Id)
        }
    }

    "should delete scope by ID" {
        runTest {
            val repository = InMemoryScopeRepository()
            val scopeId = ScopeId.generate()

            val scope = createTestScope(id = scopeId, title = "Test Scope", description = null, parentId = null)
            repository.save(scope)

            // Verify it exists
            val existsResult = repository.existsById(scopeId)
            existsResult.shouldBeRight() shouldBe true

            // Delete it
            val deleteResult = repository.deleteById(scopeId)
            deleteResult.shouldBeRight()

            // Verify it no longer exists
            val existsAfterDeleteResult = repository.existsById(scopeId)
            existsAfterDeleteResult.shouldBeRight() shouldBe false
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

            // Test whitespace variations (only valid titles without newlines)
            val validTestCases = listOf(
                "task name",         // Normalized
                " task name ",       // Leading/trailing spaces
                "  TASK NAME  ",     // Case and multiple spaces
                " Task Name ",       // Original case with spaces
                "Task  Name",        // Internal double space
                "Task\tName",        // Internal tab (tabs are allowed)
                "Task   \t  Name"    // Mixed internal whitespace (excluding newlines)
            )

            validTestCases.forEach { testTitle ->
                val existsResult = repository.existsByParentIdAndTitle(parentId, testTitle)
                val exists = existsResult.shouldBeRight()
                exists shouldBe true
            }

            // Test that titles with newlines are considered invalid and don't match
            val invalidTestCases = listOf(
                "Task\nName",        // Internal newline
                "\ttask name\n",     // Tab and newline
                "\t Task \n Name \r" // Complex mixed whitespace with newlines
            )

            invalidTestCases.forEach { testTitle ->
                val existsResult = repository.existsByParentIdAndTitle(parentId, testTitle)
                val exists = existsResult.shouldBeRight()
                exists shouldBe false // These are invalid titles, so they shouldn't exist
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
                "\tSpaced Title"  // Tab is OK, but not newline
            )

            shouldMatch.forEach { testTitle ->
                val existsResult = repository.existsByParentIdAndTitle(parentId, testTitle)
                val exists = existsResult.shouldBeRight()
                exists shouldBe true
            }

            // Titles with newlines should not match (they're invalid)
            val invalidTitles = listOf(
                "\tSpaced Title\n",
                "Spaced\nTitle"
            )

            invalidTitles.forEach { testTitle ->
                val existsResult = repository.existsByParentIdAndTitle(parentId, testTitle)
                val exists = existsResult.shouldBeRight()
                exists shouldBe false  // Invalid titles can't exist
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

