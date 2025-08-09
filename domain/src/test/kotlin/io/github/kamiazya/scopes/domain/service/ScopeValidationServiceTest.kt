package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.valueobject.ScopeDescription
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
