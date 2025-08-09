package io.github.kamiazya.scopes.domain.service

import arrow.core.right
import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest

/**
 * Tests for ScopeValidationService focusing on business rule validation.
 *
 * Note: These are unit tests that focus on the core business rule validation logic.
 * Integration tests with actual repository implementations are in the application layer.
 */
class ScopeValidationServiceTest : StringSpec({

    "validateTitleUniquenessWithContext should reject duplicate titles at all levels" {
        // Test the pure function validateTitleUniquenessWithContext
        // New business rule: ALL levels require unique titles

        val title = "My Project"
        val parentId = null // Root level
        val existsInParentContext = true // Duplicate exists

        // When: Validating title uniqueness at root level with duplicate
        val result = ScopeValidationService.validateTitleUniquenessWithContext(
            existsInParentContext = existsInParentContext,
            title = title,
            parentId = parentId
        )

        // Then: Should fail because duplicates are now forbidden at all levels
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

    "validateTitleUniquenessWithContext should allow unique titles at all levels" {
        // Test acceptance of unique titles at both root and child levels

        // Root level unique title
        val rootTitle = "Unique Root Project"
        val rootResult = ScopeValidationService.validateTitleUniquenessWithContext(
            existsInParentContext = false, // No duplicate
            title = rootTitle,
            parentId = null // Root level
        )
        rootResult.isRight() shouldBe true

        // Child level unique title
        val childTitle = "Unique Task Name"
        val parentId = ScopeId.generate()
        val childResult = ScopeValidationService.validateTitleUniquenessWithContext(
            existsInParentContext = false, // No duplicate in parent context
            title = childTitle,
            parentId = parentId
        )
        childResult.isRight() shouldBe true
    }

    "validateTitleUniquenessWithContext should demonstrate consistent uniqueness behavior" {
        // This test demonstrates the new unified business rule

        // Example 1: Root-level projects must have unique names
        val projectTitle = "Website Project"
        val rootDuplicateResult = ScopeValidationService.validateTitleUniquenessWithContext(
            existsInParentContext = true, // Another "Website Project" already exists
            title = projectTitle,
            parentId = null // Root level
        )
        rootDuplicateResult.isLeft() shouldBe true // Should be rejected

        // Example 2: Root-level projects with unique names are allowed
        val uniqueRootResult = ScopeValidationService.validateTitleUniquenessWithContext(
            existsInParentContext = false, // No duplicate exists
            title = "Personal Portfolio Website",
            parentId = null // Root level
        )
        uniqueRootResult.isRight() shouldBe true // Should be allowed

        // Example 3: Child tasks must have unique names within their parent
        val taskTitle = "Implementation"
        val parentA = ScopeId.generate()

        val childDuplicateResult = ScopeValidationService.validateTitleUniquenessWithContext(
            existsInParentContext = true, // Duplicate exists under same parent
            title = taskTitle,
            parentId = parentA
        )
        childDuplicateResult.isLeft() shouldBe true // Should be rejected

        // Example 4: Child tasks with unique names within parent are allowed
        val uniqueChildResult = ScopeValidationService.validateTitleUniquenessWithContext(
            existsInParentContext = false, // No duplicate under parent
            title = "Backend Implementation",
            parentId = parentA
        )
        uniqueChildResult.isRight() shouldBe true // Should be allowed
    }
})
