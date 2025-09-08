package io.github.kamiazya.scopes.scopemanagement.domain.entity

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Instant

/**
 * Tests for Scope entity focusing on business behavior and user scenarios.
 *
 * A Scope represents a unit of work organization in the task management system.
 * It can be a project, epic, or task - all unified under the same entity.
 */
class ScopeTest :
    StringSpec({
        val testTime = Instant.parse("2024-01-01T12:00:00Z")
        val laterTime = Instant.parse("2024-01-01T14:00:00Z")

        "user can organize work by creating new scopes for projects and tasks" {
            // When a user wants to create a new project or task to organize their work
            val workScope = Scope.create(
                title = "Website Redesign Project",
                description = "Complete overhaul of company website with modern design",
                parentId = null,
                now = testTime,
            )

            // Then the scope is created successfully with their specifications
            val result = workScope.shouldBeRight()
            result.title.value shouldBe "Website Redesign Project"
            result.description?.value shouldBe "Complete overhaul of company website with modern design"
            result.parentId shouldBe null
        }

        "system prevents creating scopes without meaningful titles to maintain organization clarity" {
            // When a user tries to create a scope without providing a clear title
            val unclearScope = Scope.create(
                title = "",
                description = "Some work to be done",
                parentId = null,
                now = testTime,
            )

            // Then the system prevents this to ensure all work items have clear identification
            unclearScope.shouldBeLeft()
        }

        "system enforces title length limits to maintain readability in project views" {
            // When a user tries to create a scope with an excessively long title
            val excessiveTitle = "a".repeat(201) // Over the reasonable display limit
            val scopeWithLongTitle = Scope.create(
                title = excessiveTitle,
                description = "Project description",
                parentId = null,
                now = testTime,
            )

            // Then the system prevents this to maintain clean, readable project hierarchies
            scopeWithLongTitle.shouldBeLeft()
        }

        "system automatically cleans up user input to improve data quality" {
            // When a user creates a scope with extra whitespace in the title
            val scopeWithWhitespace = Scope.create(
                title = "  Mobile App Development  ",
                description = "Native iOS and Android app",
                parentId = null,
                now = testTime,
            )

            // Then the system normalizes the input for consistency
            val normalizedScope = scopeWithWhitespace.shouldBeRight()
            normalizedScope.title.value shouldBe "Mobile App Development"
        }

        "user can update scope titles to reflect changing project requirements" {
            // Given an existing project scope
            val initialScope = Scope.create(
                title = "Q4 Planning",
                description = "Planning for Q4 2024",
                parentId = null,
                now = testTime,
            ).shouldBeRight()

            // When requirements change and the user updates the title
            val updatedScope = initialScope.updateTitle("Q4 Planning - Extended to Q1 2025", laterTime)

            // Then the scope reflects the new title while maintaining its identity
            val result = updatedScope.shouldBeRight()
            result.title.value shouldBe "Q4 Planning - Extended to Q1 2025"
            result.id shouldBe initialScope.id
            result.updatedAt.toEpochMilliseconds() >= initialScope.updatedAt.toEpochMilliseconds()
        }

        "user can update descriptions to provide better context for team members" {
            // Given a scope with minimal description
            val initialScope = Scope.create(
                title = "API Integration",
                description = "Basic integration",
                parentId = null,
                now = testTime,
            ).shouldBeRight()

            // When the user adds more detailed information
            val detailedDescription = "Integration with payment gateway API including Stripe, PayPal, and Square"
            val updatedScope = initialScope.updateDescription(detailedDescription, laterTime)

            // Then team members have better context about the work
            val result = updatedScope.shouldBeRight()
            result.description?.value shouldBe detailedDescription
            result.updatedAt.toEpochMilliseconds() >= initialScope.updatedAt.toEpochMilliseconds()
        }

        "user can clear descriptions when they become outdated or irrelevant" {
            // Given a scope with a description that's no longer relevant
            val scopeWithDescription = Scope.create(
                title = "Research Task",
                description = "Initial research notes that are now outdated",
                parentId = null,
                now = testTime,
            ).shouldBeRight()

            // When the user clears the outdated information
            val clearedScope = scopeWithDescription.updateDescription(null, laterTime)

            // Then the scope has no description
            val result = clearedScope.shouldBeRight()
            result.description shouldBe null
        }
    })
