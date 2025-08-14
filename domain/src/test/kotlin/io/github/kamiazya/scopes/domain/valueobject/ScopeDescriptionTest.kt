package io.github.kamiazya.scopes.domain.valueobject

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.ScopeValidationError

/**
 * Tests for ScopeDescription focusing on user content creation and business context.
 *
 * Descriptions allow users to provide additional context for their work organization.
 * Unlike titles, descriptions are optional and support rich content for detailed explanation.
 */
class ScopeDescriptionTest : StringSpec({

    "users can provide detailed context for their work with rich descriptions" {
        // When a user wants to document the purpose and details of their project
        val result = ScopeDescription.create(
            "This project focuses on improving user authentication security " +
            "by implementing OAuth 2.0 integration with our existing systems."
        )

        // Then the system preserves their detailed context for future reference
        result.shouldBeRight()!!.value shouldBe "This project focuses on improving user authentication security " +
            "by implementing OAuth 2.0 integration with our existing systems."
    }

    "system automatically formats descriptions for clean presentation" {
        // When a user enters description content with extra whitespace
        val result = ScopeDescription.create("  Clean up the API endpoints  ")

        // Then the system formats it consistently for better readability
        result.shouldBeRight()!!.value shouldBe "Clean up the API endpoints"
    }

    "users can choose to create scopes without descriptions when context is obvious" {
        // When a user doesn't need additional context beyond the title
        val result = ScopeDescription.create(null)

        // Then the system allows minimal scope creation without forcing unnecessary content
        result.shouldBeRight() shouldBe null
    }

    "system treats empty description input as user's choice to omit additional context" {
        // When a user leaves the description field empty in a form
        val result = ScopeDescription.create("")

        // Then the system respects this as intentionally minimal scope creation
        result.shouldBeRight() shouldBe null
    }

    "system handles whitespace-only descriptions as user's choice for no additional context" {
        // When a user accidentally enters only spaces in the description field
        val result = ScopeDescription.create("   ")

        // Then the system treats this as no meaningful content provided
        result.shouldBeRight() shouldBe null
    }

    "system prevents excessively long descriptions that could impact system performance" {
        // When a user tries to create a description that exceeds reasonable limits
        val massiveDescription = "a".repeat(1001) // Beyond reasonable documentation length
        val result = ScopeDescription.create(massiveDescription)

        // Then the system prevents this to maintain good system performance and UX
        val error = result.shouldBeLeft()
        error shouldBe ScopeValidationError.ScopeDescriptionTooLong(1000, 1001)
    }

    "system supports comprehensive documentation up to reasonable limits" {
        // When a user needs to provide extensive but reasonable documentation
        val comprehensiveDescription = buildString {
            append("Project Overview: This is a complex project requiring detailed documentation.\n\n")
            append("Key Requirements:\n")
            append("- User authentication and authorization\n")
            append("- Data processing and validation\n")
            append("- Integration with external APIs\n")
            append("- Performance optimization\n\n")
            append("Technical Approach:\n")
            append("- Microservices architecture\n")
            append("- Event-driven design patterns\n")
            append("- Comprehensive testing strategy\n\n")
            append("Timeline and Milestones:\n")
            append("- Phase 1: Core infrastructure (4 weeks)\n")
            append("- Phase 2: Feature implementation (6 weeks)\n")
            append("- Phase 3: Testing and optimization (2 weeks)\n\n")
            append("Success Criteria:\n")
            append("- All functional requirements met\n")
            append("- Performance benchmarks achieved\n")
            append("- Security audit passed\n\n")
            append("Additional notes about implementation details and considerations.")
        }

        // Ensure we're at a reasonable length but under the limit
        require(comprehensiveDescription.length < 1000) { "Test description should be under limit" }

        val result = ScopeDescription.create(comprehensiveDescription)

        // Then the system allows this for users who need detailed project documentation
        result.shouldBeRight()!!.value shouldBe comprehensiveDescription
    }

    "system supports technical documentation with special characters" {
        // When users document technical details that include code snippets or technical notation
        val technicalDescription = "API endpoint: /users/{id} - Returns HTTP 200/404/500. " +
            "Query params: ?include=profile&sort=name. " +
            "Requires auth token in header: Authorization: Bearer <token>"
        val result = ScopeDescription.create(technicalDescription)

        // Then the system preserves all technical notation for accurate documentation
        result.shouldBeRight()!!.value shouldBe technicalDescription
    }

    "system supports international users with multilingual project documentation" {
        // When international users document projects in their native language
        val multilingualDescription = "プロジェクト概要: ユーザー体験の改善\n" +
            "Objectif: Améliorer l'expérience utilisateur 🚀\n" +
            "Goal: Enhanced user experience with modern interface"
        val result = ScopeDescription.create(multilingualDescription)

        // Then the system fully supports global teams and diverse communication needs
        result.shouldBeRight()!!.value shouldBe multilingualDescription
    }

    "system supports structured documentation with multi-line formatting" {
        // When users need to create well-organized, multi-line project documentation
        val structuredDescription = """
            ## Project Goals
            - Improve performance by 40%
            - Reduce load times under 2 seconds

            ## Technical Approach
            1. Optimize database queries
            2. Implement caching layer
            3. Compress static assets

            ## Success Criteria
            - Performance benchmarks met
            - No regression in functionality
        """.trimIndent()

        val result = ScopeDescription.create(structuredDescription)

        // Then the system preserves formatting for readable project documentation
        result.shouldBeRight()!!.value shouldBe structuredDescription
    }

    // These remaining tests focus on the value object contract rather than business behavior
    // They're kept minimal and focused on ensuring the object works correctly in the system

    "description value objects provide direct access to their content" {
        val description = ScopeDescription.create("Access test").shouldBeRight()!!
        description.value shouldBe "Access test"
    }

    "description value objects provide string representation for display purposes" {
        val description = ScopeDescription.create("Display test").shouldBeRight()!!
        description.toString() shouldBe "Display test"
    }

    "description value objects support equality comparison for system operations" {
        val description1 = ScopeDescription.create("Same content").shouldBeRight()!!
        val description2 = ScopeDescription.create("Same content").shouldBeRight()!!
        val description3 = ScopeDescription.create("Different content").shouldBeRight()!!
        val nullDescription1 = ScopeDescription.create(null).shouldBeRight()
        val nullDescription2 = ScopeDescription.create("").shouldBeRight()

        description1 shouldBe description2
        (description1 == description3) shouldBe false
        nullDescription1 shouldBe nullDescription2
    }

    "system handles various whitespace edge cases consistently" {
        // Edge case testing for different types of whitespace characters
        val result1 = ScopeDescription.create("\t\n\r ")
        val result2 = ScopeDescription.create("\u0020\u00A0\u2000\u2001")

        result1.shouldBeRight() shouldBe null
        result2.shouldBeRight() shouldBe null
    }
})
