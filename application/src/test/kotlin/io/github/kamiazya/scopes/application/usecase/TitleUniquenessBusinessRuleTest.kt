package io.github.kamiazya.scopes.application.usecase

import arrow.core.right
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.service.ApplicationScopeValidationService
import io.github.kamiazya.scopes.application.usecase.command.CreateScope
import io.github.kamiazya.scopes.application.usecase.handler.CreateScopeHandler
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.valueobject.ScopeTitle
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

/**
 * Tests for the specific business rule regarding title uniqueness in Claude Scope.
 *
 * This test suite focuses on verifying the key business rule for consistent uniqueness:
 * - All scopes (both root and child levels): Duplicate titles are FORBIDDEN
 */
class TitleUniquenessBusinessRuleTest : StringSpec({

    "system prevents duplicate titles at root level for clear project identification" {
        runTest {
            // Given: A user already has a "Website" project at root level
            val mockRepository = mockk<ScopeRepository>()
            val validationService = ApplicationScopeValidationService(mockRepository)
            val handler = CreateScopeHandler(mockRepository, validationService)

            // Mock repository indicating duplicate exists at root level
            coEvery {
                mockRepository.existsByParentIdAndTitle(null, "website")
            } returns true.right() // Duplicate exists

            // When: User tries to create another "Website" project at root level
            val command = CreateScope(
                title = "Website",
                description = "Another website project",
                parentId = null // Root level
            )
            val result = handler(command)

            // Then: The system prevents this to ensure clear project identification
            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<ApplicationError.DomainErrors>()

            // Verify that save was never called when validation fails
            coVerify(exactly = 0) { mockRepository.save(any()) }
        }
    }

    "users must create projects with unique descriptive names for clear identification" {
        runTest {
            // Given: A user wants to organize different types of projects
            val mockRepository = mockk<ScopeRepository>()
            val validationService = ApplicationScopeValidationService(mockRepository)
            val handler = CreateScopeHandler(mockRepository, validationService)

            // Mock repository showing no duplicate exists - all validations should pass
            coEvery {
                mockRepository.existsByParentIdAndTitle(null, "personal portfolio website")
            } returns false.right()
            // Root level projects don't need hierarchy/children validations

            // Create a valid result scope for the save operation
            val resultScope = Scope(
                id = ScopeId.generate(),
                title = ScopeTitle.create("Personal Portfolio Website")
                    .fold({ error("Invalid title in test setup") }, { it }),
                description = ScopeDescription.create("Modern React-based portfolio site")
                    .fold({ error("Invalid description in test setup") }, { it }),
                parentId = null,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now()
            )
            coEvery {
                mockRepository.save(match { scope ->
                    scope.title.value == "Personal Portfolio Website"
                })
            } returns resultScope.right()

            // When: User creates a descriptive, unique project name
            val command = CreateScope(
                title = "Personal Portfolio Website",
                description = "Modern React-based portfolio site",
                parentId = null // Root level
            )
            val result = handler(command)

            // Then: The system allows this with clear, unique naming
            result.shouldBeRight()
        }
    }

    "system prevents duplicate task names within projects to maintain clarity" {
        runTest {
            // Given: A personal project already has a task named "Database Setup"
            val mockRepository = mockk<ScopeRepository>()
            val validationService = ApplicationScopeValidationService(mockRepository)
            val handler = CreateScopeHandler(mockRepository, validationService)

            val projectId = ScopeId.generate()

            // Mock repository indicating duplicate exists within the same parent
            coEvery { mockRepository.existsById(projectId) } returns true.right()
            coEvery {
                mockRepository.existsByParentIdAndTitle(projectId, "database setup")
            } returns true.right() // Duplicate exists in same parent
            coEvery { mockRepository.findHierarchyDepth(projectId) } returns 1.right()
            coEvery { mockRepository.countByParentId(projectId) } returns 1.right()

            // When: User tries to create another "Database Setup" task in the same project
            val command = CreateScope(
                title = "Database Setup",
                description = "Another database setup task",
                parentId = projectId.toString()
            )
            val result = handler(command)

            // Then: The system prevents this to avoid confusion within the project
            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<ApplicationError.DomainErrors>()

            // Verify that save was never called when validation fails
            coVerify(exactly = 0) { mockRepository.save(any()) }
        }
    }

    "descriptive project naming prevents ambiguity and improves organization" {
        runTest {
            // Given: A developer wants to work on learning projects
            val mockRepository = mockk<ScopeRepository>()
            val validationService = ApplicationScopeValidationService(mockRepository)
            val handler = CreateScopeHandler(mockRepository, validationService)

            // Mock repository showing no duplicate exists for descriptive name - all validations should pass
            coEvery {
                mockRepository.existsByParentIdAndTitle(null, "react learning todo app")
            } returns false.right()
            // Root level projects don't need hierarchy/children validations

            // Create a valid result scope for the save operation
            val resultScope = Scope(
                id = ScopeId.generate(),
                title = ScopeTitle.create("React Learning Todo App")
                    .fold({ error("Invalid title in test setup") }, { it }),
                description = ScopeDescription.create(
                    "Tutorial-based todo application for learning React fundamentals"
                ).fold({ error("Invalid description in test setup") }, { it }),
                parentId = null,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now()
            )
            coEvery {
                mockRepository.save(match { scope ->
                    scope.title.value == "React Learning Todo App"
                })
            } returns resultScope.right()

            // When: User creates a descriptive learning project
            val command = CreateScope(
                title = "React Learning Todo App",
                description = "Tutorial-based todo application for learning React fundamentals",
                parentId = null
            )
            val result = handler(command)

            // Then: Clear naming helps maintain organized project structure
            result.shouldBeRight()
        }
    }

    "system enforces unique naming at all levels for consistent organization" {
        runTest {
            // Given: A personal project with well-organized task structure
            val mockRepository = mockk<ScopeRepository>()
            val validationService = ApplicationScopeValidationService(mockRepository)
            val handler = CreateScopeHandler(mockRepository, validationService)

            val projectId = ScopeId.generate()

            // Mock showing a "Testing" task already exists in this project
            coEvery { mockRepository.existsById(projectId) } returns true.right()
            coEvery {
                mockRepository.existsByParentIdAndTitle(projectId, "testing")
            } returns true.right() // "Testing" already exists
            coEvery { mockRepository.findHierarchyDepth(projectId) } returns 1.right()
            coEvery { mockRepository.countByParentId(projectId) } returns 2.right()

            // When: User tries to add another "Testing" task to the same project
            val command = CreateScope(
                title = "Testing",
                description = "Additional testing work",
                parentId = projectId.toString()
            )
            val result = handler(command)

            // Then: The system enforces unique naming at all levels for consistency
            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<ApplicationError.DomainErrors>()
        }
    }

    "system performs case-insensitive duplicate title checks to prevent confusion" {
        runTest {
            // Given: A user already has a "website" project (lowercase)
            val mockRepository = mockk<ScopeRepository>()
            val validationService = ApplicationScopeValidationService(mockRepository)
            val handler = CreateScopeHandler(mockRepository, validationService)

            // Mock repository indicating duplicate exists with lowercase title
            // Note: The repository should check case-insensitively
            coEvery {
                mockRepository.existsByParentIdAndTitle(null, "website")
            } returns true.right() // Repository detects "website" exists (case-insensitive)

            // When: User tries to create "Website" project with different casing
            val command = CreateScope(
                title = "Website", // Different casing
                description = "My personal website",
                parentId = null // Root level
            )
            val result = handler(command)

            // Then: The system prevents this to avoid case-based duplicates
            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<ApplicationError.DomainErrors>()

            // Verify that save was never called when validation fails
            coVerify(exactly = 0) { mockRepository.save(any()) }
        }
    }
})
