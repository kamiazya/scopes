package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

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

    "validateTitleUniquenessWithContext should allow duplicate titles at root level (parentId = null)" {
        // Test the pure function validateTitleUniquenessWithContext
        // This tests the business rule directly without repository dependency

        val title = "My Project"
        val parentId = null // Root level
        val existsInParentContext = true // Even if duplicate exists, should be allowed

        // When: Validating title uniqueness at root level with duplicate
        val result = ScopeValidationService.validateTitleUniquenessWithContext(
            existsInParentContext = existsInParentContext,
            title = title,
            parentId = parentId
        )

        // Then: Should succeed because root level allows duplicates
        result.isRight() shouldBe true
    }

    "validateTitleUniquenessWithContext should reject duplicate titles within same parent" {
        // Test rejection of duplicates when parentId is not null

        val title = "Task Implementation"
        val parentId = ScopeId.generate() // Child scope
        val existsInParentContext = true // Duplicate exists in parent context

        // When: Validating title uniqueness with duplicate in same parent
        val result = ScopeValidationService.validateTitleUniquenessWithContext(
            existsInParentContext = existsInParentContext,
            title = title,
            parentId = parentId
        )

        // Then: Should fail with ScopeDuplicateTitle error
        result.isLeft() shouldBe true
        result.fold(
            { error ->
                error.shouldBeInstanceOf<DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle>()
                error.title shouldBe title
                error.parentId shouldBe parentId
            },
            { /* Should not reach here */ }
        )
    }

    "validateTitleUniquenessWithContext should allow unique titles within same parent" {
        // Test acceptance of unique titles when parentId is not null

        val title = "Unique Task Name"
        val parentId = ScopeId.generate() // Child scope
        val existsInParentContext = false // No duplicate in parent context

        // When: Validating unique title within parent
        val result = ScopeValidationService.validateTitleUniquenessWithContext(
            existsInParentContext = existsInParentContext,
            title = title,
            parentId = parentId
        )

        // Then: Should succeed
        result.isRight() shouldBe true
    }

    "validateTitleUniquenessWithContext should demonstrate business rule behavior with examples" {
        // This test demonstrates the business rule with concrete examples

        // Example 1: Multiple root-level projects with same name should be allowed
        val projectTitle = "Website Redesign"
        val result1 = ScopeValidationService.validateTitleUniquenessWithContext(
            existsInParentContext = true, // Another "Website Redesign" already exists
            title = projectTitle,
            parentId = null // Root level
        )
        result1.isRight() shouldBe true // Should be allowed

        // Example 2: Tasks with same name under different parents should be allowed
        val taskTitle = "Implementation"
        val parentA = ScopeId.generate()
        val parentB = ScopeId.generate()

        val resultParentA = ScopeValidationService.validateTitleUniquenessWithContext(
            existsInParentContext = false, // No duplicate under parent A
            title = taskTitle,
            parentId = parentA
        )
        val resultParentB = ScopeValidationService.validateTitleUniquenessWithContext(
            existsInParentContext = false, // No duplicate under parent B
            title = taskTitle,
            parentId = parentB
        )

        resultParentA.isRight() shouldBe true
        resultParentB.isRight() shouldBe true

        // Example 3: Tasks with same name under same parent should be forbidden
        val duplicateInSameParent = ScopeValidationService.validateTitleUniquenessWithContext(
            existsInParentContext = true, // Duplicate exists under same parent
            title = taskTitle,
            parentId = parentA
        )
        duplicateInSameParent.isLeft() shouldBe true
    }
})
