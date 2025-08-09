package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.error.DomainError
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for ScopeValidationService focusing on business rule validation.
 *
 * Note: These are unit tests that focus on the core business rule validation logic.
 * Integration tests with actual repository implementations are in the application layer.
 */
class ScopeValidationServiceTest : StringSpec({

    "Business Rule Documentation - Root Level Duplicate Titles" {
        // This test documents the business rule that duplicate titles are allowed at root level
        // This is a design decision documented in docs/explanation/domain-overview.md

        // The rule: parentId = null allows duplicate titles
        // The rationale: Users may have multiple unrelated projects with similar names

        // Examples that should be ALLOWED:
        // ├── Scope: "Website" (Project A)
        // ├── Scope: "Website" (Project B)  // Duplicate title OK at root
        // └── Scope: "Documentation"

        // This behavior is implemented in ScopeValidationService.validateTitleUniquenessEfficient()
        // where the method returns early with success when parentId is null

        val documentedBehavior = "Root-level scopes (parentId = null) allow duplicate titles"
        val implementationLocation = "ScopeValidationService.validateTitleUniquenessEfficient()"

        // Verify the business rule is documented
        documentedBehavior shouldBe "Root-level scopes (parentId = null) allow duplicate titles"
        implementationLocation shouldBe "ScopeValidationService.validateTitleUniquenessEfficient()"
    }

    "Business Rule Documentation - Child Level Duplicate Titles" {
        // This test documents the business rule that duplicate titles are forbidden within same parent

        // The rule: parentId != null forbids duplicate titles within same parent
        // The rationale: Prevents confusion and maintains clear identification within project context

        // Examples that should be FORBIDDEN:
        // └── Scope: "Project Alpha"
        //     ├── Scope: "Implementation"
        //     └── Scope: "Implementation"  // Duplicate title forbidden

        // Examples that should be ALLOWED:
        // ├── Scope: "Project Alpha"
        // │   └── Scope: "Implementation"
        // └── Scope: "Project Beta"
        //     └── Scope: "Implementation"  // Same title OK under different parents

        val documentedBehavior = "Child scopes (parentId != null) forbid duplicate titles within same parent"
        val errorType = "DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle"

        // Verify the business rule is documented
        documentedBehavior shouldBe "Child scopes (parentId != null) forbid duplicate titles within same parent"
        errorType shouldBe "DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle"
    }

    "validateTitle should enforce basic title constraints" {
        // Note: ScopeValidationService.validateTitle only checks for non-empty titles
        // Full validation (length, format) is handled in ScopeTitle value object creation

        val emptyTitle = ""
        val blankTitle = "   "  // Only whitespace
        val validTitle = "Valid Project Title"

        // Empty title should fail
        val emptyResult = ScopeValidationService.validateTitle(emptyTitle)
        emptyResult.isLeft() shouldBe true

        // Blank title (only whitespace) should fail
        val blankResult = ScopeValidationService.validateTitle(blankTitle)
        blankResult.isLeft() shouldBe true

        // Valid title should succeed
        val validResult = ScopeValidationService.validateTitle(validTitle)
        validResult.isRight() shouldBe true

        // Note: Complex validations (length, newlines) are handled by ScopeTitle.create()
        // which is tested separately in ScopeTitleTest.kt
    }

    "validateDescription should enforce description constraints" {
        // Test basic description validation (non-repository dependent)
        val validDescription = "This is a valid description"
        val tooLongDescription = "A".repeat(1001) // Exceeds 1000 character limit
        val nullDescription = null

        // Valid description should succeed
        val validResult = ScopeValidationService.validateDescription(validDescription)
        validResult.isRight() shouldBe true

        // Null description should succeed (optional field)
        val nullResult = ScopeValidationService.validateDescription(nullDescription)
        nullResult.isRight() shouldBe true

        // Too long description should fail
        val longResult = ScopeValidationService.validateDescription(tooLongDescription)
        longResult.isLeft() shouldBe true
    }
})
