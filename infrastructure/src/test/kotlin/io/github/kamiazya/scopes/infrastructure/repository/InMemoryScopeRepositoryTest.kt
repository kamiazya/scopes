package io.github.kamiazya.scopes.infrastructure.repository

import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

class InMemoryScopeRepositoryTest : StringSpec({

    "should save scope" {
        runTest {
            val repository = InMemoryScopeRepository()
            val scope = Scope(
                id = ScopeId.generate(),
                title = "Test Scope",
                description = "Test Description",
                parentId = null,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now()
            )

            val saveResult = repository.save(scope)
            val savedScope = saveResult.shouldBeRight()
            savedScope shouldBe scope
        }
    }

    "should check if scope exists" {
        runTest {
            val repository = InMemoryScopeRepository()
            val scope = Scope(
                id = ScopeId.generate(),
                title = "Test Scope",
                description = "Test Description",
                parentId = null,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now()
            )

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
            val scope = Scope(
                id = ScopeId.generate(),
                title = "Test Scope",
                description = null,
                parentId = parentId,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now()
            )

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

            val child1 = Scope(
                id = ScopeId.generate(),
                title = "Child 1",
                description = null,
                parentId = parentId,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now()
            )

            val child2 = Scope(
                id = ScopeId.generate(),
                title = "Child 2",
                description = null,
                parentId = parentId,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now()
            )

            repository.save(child1)
            repository.save(child2)

            val countResult = repository.countByParentId(parentId)
            val count = countResult.shouldBeRight()
            count shouldBe 2
        }
    }

    "should calculate hierarchy depth" {
        runTest {
            val repository = InMemoryScopeRepository()
            val rootId = ScopeId.generate()
            val child1Id = ScopeId.generate()
            val child2Id = ScopeId.generate()

            val root = Scope(
                id = rootId,
                title = "Root",
                description = null,
                parentId = null,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now()
            )

            val child1 = Scope(
                id = child1Id,
                title = "Child 1",
                description = null,
                parentId = rootId,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now()
            )

            val child2 = Scope(
                id = child2Id,
                title = "Child 2",
                description = null,
                parentId = child1Id,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now()
            )

            repository.save(root)
            repository.save(child1)
            repository.save(child2)

            val depthResult = repository.findHierarchyDepth(child2Id)
            val depth = depthResult.shouldBeRight()
            depth shouldBe 3
        }
    }
})
