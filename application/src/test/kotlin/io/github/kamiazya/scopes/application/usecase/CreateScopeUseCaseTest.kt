package io.github.kamiazya.scopes.application.usecase

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.RepositoryError
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock

class CreateScopeUseCaseTest : StringSpec({

    "should create scope successfully with valid request" {
        runTest {
            val mockRepository = mockk<ScopeRepository>()
            val useCase = CreateScopeUseCase(mockRepository)

            val testScopeId = ScopeId.from("01ARZ3NDEKTSV4RRFFQ69G5FAV")
            val savedScope = Scope(
                id = testScopeId,
                title = "Test Scope",
                description = "Test Description",
                parentId = null,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )

            // Mock the efficient validation methods
            coEvery { mockRepository.existsByParentIdAndTitle(null, "Test Scope") } returns false.right()
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
            error.shouldBeInstanceOf<ApplicationError.Domain>()
        }
    }

    "should fail with repository error when save fails" {
        runTest {
            val mockRepository = mockk<ScopeRepository>()
            val useCase = CreateScopeUseCase(mockRepository)

            // Mock the efficient validation methods - only existsByParentIdAndTitle is called for null parentId
            coEvery { mockRepository.existsByParentIdAndTitle(null, "Test Scope") } returns false.right()
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
            error.shouldBeInstanceOf<ApplicationError.Repository>()
        }
    }

    "should fail when parent scope does not exist" {
        runTest {
            val mockRepository = mockk<ScopeRepository>()
            val useCase = CreateScopeUseCase(mockRepository)

            val parentId = ScopeId.from("01ARZ3NDEKTSV4RRFFQ69G5FB0")

            // Mock existsById to return false so parent doesn't exist
            coEvery { mockRepository.existsById(parentId) } returns false.right()

            val request = CreateScopeRequest(
                title = "Test Scope",
                description = "Test Description",
                parentId = parentId
            )

            val result = useCase.execute(request)

            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<ApplicationError.Domain>()
        }
    }

    "should fail with duplicate title in same parent" {
        runTest {
            val mockRepository = mockk<ScopeRepository>()
            val useCase = CreateScopeUseCase(mockRepository)

            // Mock the efficient validation methods to simulate duplicate title
            coEvery { mockRepository.existsByParentIdAndTitle(null, "Duplicate Title") } returns true.right()

            val request = CreateScopeRequest(
                title = "Duplicate Title",
                description = "Test Description",
                parentId = null
            )

            val result = useCase.execute(request)

            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<ApplicationError.Domain>()
        }
    }
})
