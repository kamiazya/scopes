package io.github.kamiazya.scopes.application.usecase

import arrow.core.right
import io.github.kamiazya.scopes.application.service.ApplicationScopeValidationService
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.error.validationSuccess
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.valueobject.ScopeTitle
import io.github.kamiazya.scopes.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock

/**
 * Tests for CreateScopeUseCase focusing on user scenarios and business workflows.
 */
class CreateScopeUseCaseTest : StringSpec({

    "users can create new projects to organize their work and track progress" {
        runTest {
            // Given a user wants to start organizing a new project
            val mockRepository = mockk<ScopeRepository>()
            val mockValidationService = mockk<ApplicationScopeValidationService>()
            val useCase = CreateScopeUseCase(mockRepository, mockValidationService)

            // Mock validation to return success
            coEvery {
                mockValidationService.validateScopeCreation(
                    "Website Redesign Project",
                    "Complete overhaul of company website with modern UX",
                    null
                )
            } returns Unit.validationSuccess()

            // And the system has the necessary infrastructure ready
            val expectedScopeId = ScopeId.from("01ARZ3NDEKTSV4RRFFQ69G5FAV")
            val createdScope = Scope(
                id = expectedScopeId,
                title = ScopeTitle.create("Website Redesign Project").getOrNull()
                    ?: error("Failed to create test ScopeTitle for 'Website Redesign Project'"),
                description = ScopeDescription
                    .create("Complete overhaul of company website with modern UX").getOrNull(),
                parentId = null,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )

            coEvery { mockRepository.save(any()) } returns createdScope.right()

            // When they create a new project
            val request = CreateScopeRequest(
                title = "Website Redesign Project",
                description = "Complete overhaul of company website with modern UX",
                parentId = null
            )
            val result = useCase.execute(request)

            // Then they can successfully start organizing their work
            val response = result.shouldBeRight()
            response.scope.title.value shouldBe "Website Redesign Project"
            response.scope.description?.value shouldBe "Complete overhaul of company website with modern UX"
            response.scope.parentId shouldBe null
        }
    }
})
