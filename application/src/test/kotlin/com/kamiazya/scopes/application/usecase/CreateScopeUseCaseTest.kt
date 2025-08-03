package com.kamiazya.scopes.application.usecase

import arrow.core.left
import arrow.core.right
import com.kamiazya.scopes.application.error.ApplicationError
import com.kamiazya.scopes.domain.entity.Scope
import com.kamiazya.scopes.domain.entity.ScopeId
import com.kamiazya.scopes.domain.error.DomainError
import com.kamiazya.scopes.domain.error.RepositoryError
import com.kamiazya.scopes.domain.repository.ScopeRepository
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

class CreateScopeUseCaseTest : StringSpec({

    "should create scope successfully with valid request" {
        runTest {
            val mockRepository = mockk<ScopeRepository>()
            val useCase = CreateScopeUseCase(mockRepository)

            val savedScope = Scope(
                id = ScopeId.generate(),
                title = "Test Scope",
                description = "Test Description",
                parentId = null,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now()
            )

            coEvery { mockRepository.findAll() } returns emptyList<Scope>().right()
            coEvery { mockRepository.save(any()) } returns savedScope.right()

            val request = CreateScopeRequest(
                title = "Test Scope",
                description = "Test Description",
                parentId = null
            )

            val result = useCase.execute(request)

            val response = result.shouldBeRight()
            response.scope.title shouldBe "Test Scope"
            response.scope.description shouldBe "Test Description"
            response.scope.parentId shouldBe null

            coVerify { mockRepository.save(any()) }
        }
    }

    "should fail with validation error for empty title" {
        runTest {
            val mockRepository = mockk<ScopeRepository>()
            val useCase = CreateScopeUseCase(mockRepository)

            val request = CreateScopeRequest(
                title = "",
                description = "Test Description",
                parentId = null
            )

            val result = useCase.execute(request)

            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<ApplicationError.DomainError>()
        }
    }

    "should fail with repository error when save fails" {
        runTest {
            val mockRepository = mockk<ScopeRepository>()
            val useCase = CreateScopeUseCase(mockRepository)

            coEvery { mockRepository.findAll() } returns emptyList<Scope>().right()
            coEvery { mockRepository.save(any()) } returns RepositoryError.ConnectionError(
                RuntimeException("Database connection failed")
            ).left()

            val request = CreateScopeRequest(
                title = "Test Scope",
                description = "Test Description",
                parentId = null
            )

            val result = useCase.execute(request)

            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<ApplicationError.RepositoryError>()
        }
    }

    "should fail when parent scope does not exist" {
        runTest {
            val mockRepository = mockk<ScopeRepository>()
            val useCase = CreateScopeUseCase(mockRepository)

            val parentId = ScopeId.generate()

            coEvery { mockRepository.findAll() } returns emptyList<Scope>().right()
            coEvery { mockRepository.existsById(parentId) } returns false.right()

            val request = CreateScopeRequest(
                title = "Test Scope",
                description = "Test Description",
                parentId = parentId
            )

            val result = useCase.execute(request)

            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<ApplicationError.DomainError>()
        }
    }

    "should fail with duplicate title in same parent" {
        runTest {
            val mockRepository = mockk<ScopeRepository>()
            val useCase = CreateScopeUseCase(mockRepository)

            val existingScope = Scope(
                id = ScopeId.generate(),
                title = "Duplicate Title",
                description = null,
                parentId = null,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now()
            )

            coEvery { mockRepository.findAll() } returns listOf(existingScope).right()

            val request = CreateScopeRequest(
                title = "Duplicate Title",
                description = "Test Description",
                parentId = null
            )

            val result = useCase.execute(request)

            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<ApplicationError.DomainError>()
        }
    }
})
