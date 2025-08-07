package io.github.kamiazya.scopes.application.usecase

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.valueobject.ScopeTitle
import io.github.kamiazya.scopes.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.RepositoryError
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock

/**
 * Tests for CreateScopeUseCase focusing on user scenarios and business workflows.
 *
 * This use case represents the primary way users organize their work by creating
 * new projects, tasks, and organizational structures in the system.
 */
class CreateScopeUseCaseTest : StringSpec({

    "users can create new projects to organize their work and track progress" {
        runTest {
            // Given a user wants to start organizing a new project
            val mockRepository = mockk<ScopeRepository>()
            val useCase = CreateScopeUseCase(mockRepository)

            // And the system has the necessary infrastructure ready
            val expectedScopeId = ScopeId.from("01ARZ3NDEKTSV4RRFFQ69G5FAV")
            val createdScope = Scope(
                id = expectedScopeId,
                title = ScopeTitle.create("Website Redesign Project").getOrNull()
                    ?: error("Failed to create test ScopeTitle for 'Website Redesign Project'"),
                description = ScopeDescription.create("Complete overhaul of company website with modern UX").getOrNull(),
                parentId = null,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )

            // And the project name is available (no conflicts)
            coEvery { mockRepository.existsByParentIdAndTitle(null, "Website Redesign Project") } returns false.right()
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

    "system prevents users from creating unclear work organization without meaningful titles" {
        runTest {
            // Given a user tries to create a project without providing a clear title
            val mockRepository = mockk<ScopeRepository>()
            val useCase = CreateScopeUseCase(mockRepository)

            // And the system needs to validate their input
            coEvery { mockRepository.existsByParentIdAndTitle(null, "") } returns false.right()

            // When they attempt to create a scope without a meaningful identifier
            val request = CreateScopeRequest(
                title = "", // No clear identification for the work
                description = "Some project work needs to be done",
                parentId = null
            )
            val result = useCase.execute(request)

            // Then the system prevents this to ensure all work items are clearly identifiable
            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<ApplicationError.Domain>()
        }
    }

    "system provides clear feedback when infrastructure issues prevent work organization" {
        runTest {
            // Given a user wants to create a legitimate project
            val mockRepository = mockk<ScopeRepository>()
            val useCase = CreateScopeUseCase(mockRepository)

            // And their request passes all business validations
            coEvery { mockRepository.existsByParentIdAndTitle(null, "Infrastructure Project") } returns false.right()

            // But the system encounters a technical issue during save
            coEvery { mockRepository.save(any()) } returns RepositoryError.ConnectionError(
                RuntimeException("Database connection temporarily unavailable")
            ).left()

            // When they attempt to create their project
            val request = CreateScopeRequest(
                title = "Infrastructure Project",
                description = "Upgrade server infrastructure for better performance",
                parentId = null
            )
            val result = useCase.execute(request)

            // Then the system clearly indicates the technical issue (not a user error)
            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<ApplicationError.Repository>()
        }
    }

    "system prevents creating subtasks under non-existent projects to maintain organizational integrity" {
        runTest {
            // Given a user tries to create a subtask under a project that doesn't exist
            val mockRepository = mockk<ScopeRepository>()
            val useCase = CreateScopeUseCase(mockRepository)

            val nonExistentParentId = ScopeId.from("01ARZ3NDEKTSV4RRFFQ69G5FB0")

            // And the referenced parent project doesn't exist in the system
            coEvery { mockRepository.existsById(nonExistentParentId) } returns false.right()

            // When they attempt to create the subtask
            val request = CreateScopeRequest(
                title = "API Integration Task",
                description = "Integrate with third-party payment service",
                parentId = nonExistentParentId
            )
            val result = useCase.execute(request)

            // Then the system prevents this to maintain organizational integrity
            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<ApplicationError.Domain>()
        }
    }

    "system prevents naming conflicts within project boundaries to avoid user confusion" {
        runTest {
            // Given a user wants to create a new task within an existing project
            val mockRepository = mockk<ScopeRepository>()
            val useCase = CreateScopeUseCase(mockRepository)

            val existingProjectId = ScopeId.from("01ARZ3NDEKTSV4RRFFQ69G5FB0")

            // And the parent project exists with reasonable organizational limits
            coEvery { mockRepository.existsById(existingProjectId) } returns true.right()
            coEvery { mockRepository.findHierarchyDepth(existingProjectId) } returns 5.right()
            coEvery { mockRepository.countByParentId(existingProjectId) } returns 10.right()

            // But a task with that name already exists in this project
            coEvery { mockRepository.existsByParentIdAndTitle(existingProjectId, "User Authentication") } returns true.right()

            // When they try to create another task with the same name
            val request = CreateScopeRequest(
                title = "User Authentication",  // Already exists in this project
                description = "Implement OAuth login flow",
                parentId = existingProjectId
            )
            val result = useCase.execute(request)

            // Then the system prevents this to avoid confusion in project navigation
            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<ApplicationError.Domain>()
        }
    }

    // User Experience: Error accumulation for better feedback
    "system provides comprehensive feedback about all input issues to improve user experience" {
        runTest {
            // Given a user makes multiple input mistakes when creating a project
            val mockRepository = mockk<ScopeRepository>()
            val useCase = CreateScopeUseCase(mockRepository)

            // And the system is configured to provide comprehensive feedback
            coEvery { mockRepository.existsByParentIdAndTitle(null, "") } returns false.right()

            // When they submit a request with multiple issues
            val request = CreateScopeRequest(
                title = "", // Missing clear identification
                description = "A".repeat(1001), // Excessively long documentation that would impact performance
                parentId = null
            )
            val result = useCase.execute(request)

            // Then the system provides feedback about all issues at once (better UX than one-at-a-time)
            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<ApplicationError.ValidationFailure>()
            error.errors.size shouldBe 2 // All input issues identified

            // And includes specific guidance for each issue
            val errorTypes = error.errors.map { it::class.simpleName }
            errorTypes shouldContainExactlyInAnyOrder listOf("EmptyTitle", "DescriptionTooLong")
        }
    }

    "should accumulate all validation errors when validation mode is ACCUMULATE" {
        runTest {
            val mockRepository = mockk<ScopeRepository>()
            val useCase = CreateScopeUseCase(mockRepository)

            val parentId = ScopeId.from("01ARZ3NDEKTSV4RRFFQ69G5FB0")

            // Mock parent exists check
            coEvery { mockRepository.existsById(parentId) } returns true.right()

            // Mock hierarchy validation
            coEvery { mockRepository.findHierarchyDepth(parentId) } returns 5.right()
            coEvery { mockRepository.countByParentId(parentId) } returns 10.right()

            // Set up multiple validation failures
            coEvery { mockRepository.existsByParentIdAndTitle(parentId, "InvalidTitle") } returns true.right() // Duplicate title

            val request = CreateScopeRequest(
                title = "InvalidTitle", // Duplicate title - will cause error
                description = "A".repeat(1001), // Too long description - will cause error
                parentId = parentId
            )

            val result = useCase.execute(request)

            val error = result.shouldBeLeft()

            // Should get ValidationFailure with multiple errors
            error.shouldBeInstanceOf<ApplicationError.ValidationFailure>()
            error.errors.size shouldBe 2 // Description too long + duplicate title

            // Verify specific errors are included
            val errorTypes = error.errors.map { it::class.simpleName }
            errorTypes shouldContainExactlyInAnyOrder listOf("DescriptionTooLong", "DuplicateTitle")
        }
    }

    "should accumulate all validation errors by default" {
        runTest {
            val mockRepository = mockk<ScopeRepository>()
            val useCase = CreateScopeUseCase(mockRepository)

            // Mock repository calls
            coEvery { mockRepository.existsByParentIdAndTitle(null, "") } returns false.right()

            val request = CreateScopeRequest(
                title = "", // Empty title - first validation error
                description = "A".repeat(1001), // Too long description - second validation error
                parentId = null
            )

            val result = useCase.execute(request)

            val error = result.shouldBeLeft()

            // System should accumulate all validation errors
            error.shouldBeInstanceOf<ApplicationError.ValidationFailure>()
            error.errors.size shouldBe 2 // Both errors should be reported

            // Verify specific errors are included
            val errorTypes = error.errors.map { it::class.simpleName }
            errorTypes shouldContainExactlyInAnyOrder listOf("EmptyTitle", "DescriptionTooLong")
        }
    }
})
