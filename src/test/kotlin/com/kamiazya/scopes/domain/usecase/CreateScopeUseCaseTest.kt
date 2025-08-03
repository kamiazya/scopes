package com.kamiazya.scopes.domain.usecase

import com.kamiazya.scopes.domain.entity.Priority
import com.kamiazya.scopes.domain.entity.Scope
import com.kamiazya.scopes.domain.entity.ScopeId
import com.kamiazya.scopes.domain.repository.ScopeRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class CreateScopeUseCaseTest :
    FunSpec({
        val mockRepository = mockk<ScopeRepository>()
        val useCase = CreateScopeUseCase(mockRepository)

        beforeEach {
            io.mockk.clearAllMocks()
        }

        test("should create scope successfully") {
            val request =
                CreateScopeRequest(
                    title = "Test Scope",
                    description = "Test Description",
                    priority = Priority.HIGH,
                )

            val savedScope =
                Scope(
                    id = ScopeId.generate(),
                    title = "Test Scope",
                    description = "Test Description",
                    priority = Priority.HIGH,
                )
            coEvery { mockRepository.save(any()) } returns savedScope

            val response = useCase.execute(request)

            response.scope shouldNotBe null
            response.scope.title shouldBe "Test Scope"
            response.scope.description shouldBe "Test Description"
            response.scope.priority shouldBe Priority.HIGH

            coVerify { mockRepository.save(any()) }
        }

        test("should validate parent exists when parentId is provided") {
            val parentId = ScopeId.generate()
            val request =
                CreateScopeRequest(
                    title = "Child Scope",
                    parentId = parentId,
                )

            coEvery { mockRepository.findById(parentId) } returns null

            shouldThrow<IllegalArgumentException> {
                useCase.execute(request)
            }

            coVerify { mockRepository.findById(parentId) }
        }

        test("should create scope with valid parent") {
            val parentId = ScopeId.generate()
            val parent =
                Scope(
                    id = parentId,
                    title = "Parent Scope",
                )
            val request =
                CreateScopeRequest(
                    title = "Child Scope",
                    parentId = parentId,
                )

            coEvery { mockRepository.findById(parentId) } returns parent
            val childScope =
                Scope(
                    id = ScopeId.generate(),
                    title = "Child Scope",
                    parentId = parentId,
                )
            coEvery { mockRepository.save(any()) } returns childScope

            val response = useCase.execute(request)

            response.scope.parentId shouldBe parentId

            coVerify { mockRepository.findById(parentId) }
            coVerify { mockRepository.save(any()) }
        }
    })
