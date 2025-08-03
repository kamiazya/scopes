package com.kamiazya.scopes.infrastructure.repository

import com.kamiazya.scopes.domain.entity.Scope
import com.kamiazya.scopes.domain.entity.ScopeId
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest

class InMemoryScopeRepositoryTest : StringSpec({

    "should save and retrieve scope" {
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
            saveResult.shouldBeRight()

            val findResult = repository.findById(scope.id)
            val foundScope = findResult.shouldBeRight()
            foundScope shouldNotBe null
            foundScope!!.id shouldBe scope.id
            foundScope.title shouldBe scope.title
        }
    }

    "should return null for non-existent scope" {
        runTest {
            val repository = InMemoryScopeRepository()
            val nonExistentId = ScopeId.generate()

            val result = repository.findById(nonExistentId)
            val foundScope = result.shouldBeRight()
            foundScope shouldBe null
        }
    }

    "should find all scopes" {
        runTest {
            val repository = InMemoryScopeRepository()
            val scope1 = Scope(
                id = ScopeId.generate(),
                title = "Scope 1",
                description = null,
                parentId = null,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now()
            )
            val scope2 = Scope(
                id = ScopeId.generate(),
                title = "Scope 2",
                description = null,
                parentId = null,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now()
            )

            repository.save(scope1)
            repository.save(scope2)

            val result = repository.findAll()
            val scopes = result.shouldBeRight()
            scopes.size shouldBe 2
            scopes.map { it.id } shouldBe listOf(scope1.id, scope2.id)
        }
    }

    "should delete scope" {
        runTest {
            val repository = InMemoryScopeRepository()
            val scope = Scope(
                id = ScopeId.generate(),
                title = "Test Scope",
                description = null,
                parentId = null,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now()
            )

            repository.save(scope)

            val deleteResult = repository.deleteById(scope.id)
            deleteResult.shouldBeRight()

            val findResult = repository.findById(scope.id)
            val foundScope = findResult.shouldBeRight()
            foundScope shouldBe null
        }
    }

    "should update existing scope" {
        runTest {
            val repository = InMemoryScopeRepository()
            val originalScope = Scope(
                id = ScopeId.generate(),
                title = "Original Title",
                description = "Original Description",
                parentId = null,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now()
            )

            repository.save(originalScope)

            val updatedScope = originalScope.copy(
                title = "Updated Title",
                description = "Updated Description",
                updatedAt = kotlinx.datetime.Clock.System.now()
            )

            val saveResult = repository.save(updatedScope)
            saveResult.shouldBeRight()

            val findResult = repository.findById(originalScope.id)
            val foundScope = findResult.shouldBeRight()
            foundScope shouldNotBe null
            foundScope!!.title shouldBe "Updated Title"
            foundScope.description shouldBe "Updated Description"
        }
    }
})
