package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.service.ApplicationScopeValidationService
import io.github.kamiazya.scopes.application.usecase.command.CreateScope
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.error.RepositoryError
import io.github.kamiazya.scopes.domain.error.ValidationResult
import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock

class CreateScopeHandlerTest : StringSpec({

    "should create scope successfully when all validations pass" {
        runBlocking {
            // Given
            val mockRepository = mockk<ScopeRepository>()
            val mockValidationService = mockk<ApplicationScopeValidationService>()
            val handler = CreateScopeHandler(mockRepository, mockValidationService)
            
            val command = CreateScope(
                title = "Test Scope",
                description = "Test Description"
            )
            
            // Mock successful validations
            coEvery { 
                mockValidationService.validateScopeCreation(any(), any(), any()) 
            } returns ValidationResult.Success(Unit)
            
            val testId = ScopeId.generate()
            val mockScope = mockk<Scope> {
                every { id } returns testId
                every { title.value } returns "Test Scope"
                every { description?.value } returns "Test Description"
                every { parentId } returns null
                every { createdAt } returns Clock.System.now()
                every { metadata } returns emptyMap()
            }
            
            coEvery { mockRepository.save(any()) } returns mockScope.right()
            
            // When
            val result = handler(command)
            
            // Then
            result.isRight() shouldBe true
            result.onRight { createScopeResult ->
                createScopeResult.title shouldBe "Test Scope"
                createScopeResult.description shouldBe "Test Description"
                createScopeResult.parentId shouldBe null
            }
            
            coVerify { mockRepository.save(any()) }
            coVerify { mockValidationService.validateScopeCreation("Test Scope", "Test Description", null) }
        }
    }

    "should return error when parent scope does not exist" {
        runBlocking {
            // Given
            val mockRepository = mockk<ScopeRepository>()
            val mockValidationService = mockk<ApplicationScopeValidationService>()
            val handler = CreateScopeHandler(mockRepository, mockValidationService)
            
            val parentId = ScopeId.generate()
            val command = CreateScope(
                title = "Test Scope",
                parentId = parentId
            )
            
            coEvery { mockRepository.existsById(parentId) } returns false.right()
            
            // When
            val result = handler(command)
            
            // Then
            result.isLeft() shouldBe true
            result.onLeft { error ->
                error.shouldBeInstanceOf<ApplicationError.DomainErrors>()
            }
            
            coVerify { mockRepository.existsById(parentId) }
            coVerify(exactly = 0) { mockValidationService.validateScopeCreation(any(), any(), any()) }
        }
    }

    "should return error when repository validation fails" {
        runBlocking {
            // Given
            val mockRepository = mockk<ScopeRepository>()
            val mockValidationService = mockk<ApplicationScopeValidationService>()
            val handler = CreateScopeHandler(mockRepository, mockValidationService)
            
            val command = CreateScope(title = "Test Scope")
            
            coEvery { 
                mockValidationService.validateScopeCreation(any(), any(), any()) 
            } returns ValidationResult.Failure(
                arrow.core.nonEmptyListOf(DomainError.ScopeValidationError.EmptyScopeTitle)
            )
            
            // When
            val result = handler(command)
            
            // Then
            result.isLeft() shouldBe true
            result.onLeft { error ->
                error.shouldBeInstanceOf<ApplicationError.DomainErrors>()
            }
            
            coVerify(exactly = 0) { mockRepository.save(any()) }
        }
    }

    "should return error when repository save fails" {
        runBlocking {
            // Given
            val mockRepository = mockk<ScopeRepository>()
            val mockValidationService = mockk<ApplicationScopeValidationService>()
            val handler = CreateScopeHandler(mockRepository, mockValidationService)
            
            val command = CreateScope(title = "Test Scope")
            
            coEvery { 
                mockValidationService.validateScopeCreation(any(), any(), any()) 
            } returns ValidationResult.Success(Unit)
            coEvery { mockRepository.save(any()) } returns 
                RepositoryError.DatabaseError("Connection error").left()
            
            // When
            val result = handler(command)
            
            // Then
            result.isLeft() shouldBe true
            result.onLeft { error ->
                error.shouldBeInstanceOf<ApplicationError.Repository>()
            }
        }
    }

    "should validate parent exists when parentId is provided" {
        runBlocking {
            // Given
            val mockRepository = mockk<ScopeRepository>()
            val mockValidationService = mockk<ApplicationScopeValidationService>()
            val handler = CreateScopeHandler(mockRepository, mockValidationService)
            
            val parentId = ScopeId.generate()
            val command = CreateScope(
                title = "Test Scope",
                parentId = parentId
            )
            
            // Mock parent exists
            coEvery { mockRepository.existsById(parentId) } returns true.right()
            coEvery { 
                mockValidationService.validateScopeCreation(any(), any(), any()) 
            } returns ValidationResult.Success(Unit)
            
            val testId = ScopeId.generate()
            val mockScope = mockk<Scope> {
                every { id } returns testId
                every { title.value } returns "Test Scope"
                every { description?.value } returns null
                every { parentId } returns parentId
                every { createdAt } returns Clock.System.now()
                every { metadata } returns emptyMap()
            }
            coEvery { mockRepository.save(any()) } returns mockScope.right()
            
            // When
            val result = handler(command)
            
            // Then
            result.isRight() shouldBe true
            coVerify { mockRepository.existsById(parentId) }
        }
    }
})
