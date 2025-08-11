package io.github.kamiazya.scopes.domain.valueobject

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.ScopeValidationError

/**
 * Tests for ScopeTitle focusing on business rules and user experience.
 *
 * Titles are the primary way users identify and navigate their work organization.
 * These constraints ensure titles are meaningful, readable, and work well in UI contexts.
 */
class ScopeTitleTest : StringSpec({

    "users can create clear, identifiable titles for their work organization" {
        // When a user provides a clear, descriptive title for their project
        val result = ScopeTitle.create("API Integration Sprint")

        // Then the system accepts this as a valid identifier for their work
        result.shouldBeRight().value shouldBe "API Integration Sprint"
    }

    "system automatically cleans up title formatting for consistent presentation" {
        // When a user enters a title with extra whitespace (common in forms)
        val result = ScopeTitle.create("  Backend Optimization  ")

        // Then the system automatically formats it for clean presentation
        result.shouldBeRight().value shouldBe "Backend Optimization"
    }

    "system requires meaningful titles to prevent confusion in project navigation" {
        // When a user tries to create a scope without providing a title
        val result = ScopeTitle.create("")

        // Then the system prevents this to ensure all work items are identifiable
        result.shouldBeLeft() shouldBe ScopeValidationError.EmptyScopeTitle
    }

    "system prevents whitespace-only titles that would appear empty in project lists" {
        // When a user accidentally enters only spaces as a title
        val result = ScopeTitle.create("   ")

        // Then the system treats this as missing title to maintain clarity
        result.shouldBeLeft() shouldBe ScopeValidationError.EmptyScopeTitle
    }

    "system handles edge case of single space to prevent accidental empty titles" {
        // When a user enters just a single space (common typo)
        val result = ScopeTitle.create(" ")

        // Then the system prevents this to ensure intentional title creation
        result.shouldBeLeft() shouldBe ScopeValidationError.EmptyScopeTitle
    }

    "system enforces title length limits to maintain readability in user interfaces" {
        // When a user enters an excessively long title that would break UI layouts
        val excessivelyLongTitle = "a".repeat(201)
        val result = ScopeTitle.create(excessivelyLongTitle)

        // Then the system prevents this to ensure titles fit in lists, cards, and navigation
        val error = result.shouldBeLeft()
        error shouldBe ScopeValidationError.ScopeTitleTooLong(200, 201)
    }

    "system prevents line breaks in titles to maintain single-line display format" {
        // When a user accidentally includes a newline in their title
        val result = ScopeTitle.create("Multi-line\nTitle")

        // Then the system prevents this to ensure titles display correctly in lists and UI
        result.shouldBeLeft() shouldBe ScopeValidationError.ScopeTitleContainsNewline
    }

    "system prevents carriage returns in titles for consistent cross-platform behavior" {
        // When a user's input includes carriage returns (from copy-paste or different systems)
        val result = ScopeTitle.create("Title\rwith carriage return")

        // Then the system normalizes this for consistent behavior across platforms
        result.shouldBeLeft() shouldBe ScopeValidationError.ScopeTitleContainsNewline
    }

    "system allows maximum reasonable title length for detailed project identification" {
        // When a user needs a detailed but reasonable-length title
        val detailedTitle = "a".repeat(200) // At the limit but reasonable
        val result = ScopeTitle.create(detailedTitle)

        // Then the system allows this for users who need descriptive project names
        result.shouldBeRight().value shouldBe detailedTitle
    }

    "system supports diverse naming conventions with special characters" {
        // When users follow naming conventions that include special characters
        val conventionalTitle = "Project v2.1 - API Integration (Q3-2024)"
        val result = ScopeTitle.create(conventionalTitle)

        // Then the system supports common business and technical naming patterns
        result.shouldBeRight().value shouldBe conventionalTitle
    }

    "system supports international users with unicode characters and emojis" {
        // When international users create titles in their language with modern unicode
        val internationalTitle = "プロジェクト管理 - Sprint Planning 🚀"
        val result = ScopeTitle.create(internationalTitle)

        // Then the system fully supports global users and modern communication styles
        result.shouldBeRight().value shouldBe internationalTitle
    }

    // These remaining tests focus on the value object contract rather than business behavior
    // They're kept minimal and focused on ensuring the object works correctly in the system

    "title value objects provide direct access to their content" {
        val title = ScopeTitle.create("Quick Reference").shouldBeRight()
        title.value shouldBe "Quick Reference"
    }

    "title value objects provide string representation for display purposes" {
        val title = ScopeTitle.create("Display Test").shouldBeRight()
        title.toString() shouldBe "Display Test"
    }

    "title value objects support equality comparison for system operations" {
        val title1 = ScopeTitle.create("Same Name").shouldBeRight()
        val title2 = ScopeTitle.create("Same Name").shouldBeRight()
        val title3 = ScopeTitle.create("Different Name").shouldBeRight()

        title1 shouldBe title2
        (title1 == title3) shouldBe false
    }
})
