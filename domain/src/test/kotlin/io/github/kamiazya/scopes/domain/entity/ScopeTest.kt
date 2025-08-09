package io.github.kamiazya.scopes.domain.entity

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Scope entity focusing on business behavior and user scenarios.
 *
 * A Scope represents a unit of work organization in the task management system.
 * It can be a project, epic, or task - all unified under the same entity.
 */
class ScopeTest : StringSpec({

    "user can organize work by creating new scopes for projects and tasks" {
        // When a user wants to create a new project or task to organize their work
        val workScope = Scope.create(
            title = "Website Redesign Project",
            description = "Complete overhaul of company website with modern design",
            parentId = null
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
            parentId = null
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
            parentId = null
        )

        // Then the system prevents this to maintain clean, readable project hierarchies
        scopeWithLongTitle.shouldBeLeft()
    }

    "system automatically cleans up user input to improve data quality" {
        // When a user creates a scope with extra whitespace in the title
        val scopeWithWhitespace = Scope.create(
            title = "  Mobile App Development  ",
            description = "Native iOS and Android app",
            parentId = null
        )

        // Then the system automatically trims whitespace for better data quality
        val result = scopeWithWhitespace.shouldBeRight()
        result.title.value shouldBe "Mobile App Development"
    }

    "users can create scopes without descriptions when additional context is not needed" {
        // When a user creates a scope with only whitespace in the description field
        val minimalScope = Scope.create(
            title = "Quick Bug Fix",
            description = "   ", // Only whitespace
            parentId = null
        )

        // Then the system treats this as no description, keeping the scope minimal
        val result = minimalScope.shouldBeRight()
        result.description shouldBe null
    }
})
