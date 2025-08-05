package io.github.kamiazya.scopes.domain.service

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.error.DomainError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ScopeValidationServiceTest : StringSpec({

    "validateTitle should accept valid title" {
        val result = ScopeValidationService.validateTitle("Valid Title")

        val title = result.shouldBeRight()
        title shouldBe "Valid Title"
    }

    "validateTitle should trim whitespace" {
        val result = ScopeValidationService.validateTitle("  Valid Title  ")

        val title = result.shouldBeRight()
        title shouldBe "Valid Title"
    }

    "validateTitle should reject empty title" {
        val result = ScopeValidationService.validateTitle("")

        val error = result.shouldBeLeft()
        error shouldBe DomainError.ValidationError.EmptyTitle
    }

    "validateTitle should reject title too long" {
        val longTitle = "a".repeat(201)
        val result = ScopeValidationService.validateTitle(longTitle)

        val error = result.shouldBeLeft()
        error.shouldBeInstanceOf<DomainError.ValidationError.TitleTooLong>()
    }

    "validateDescription should accept null description" {
        val result = ScopeValidationService.validateDescription(null)

        val description = result.shouldBeRight()
        description shouldBe null
    }

    "validateDescription should accept valid description" {
        val result = ScopeValidationService.validateDescription("Valid description")

        val description = result.shouldBeRight()
        description shouldBe "Valid description"
    }

    "validateDescription should convert empty string to null" {
        val result = ScopeValidationService.validateDescription("   ")

        val description = result.shouldBeRight()
        description shouldBe null
    }

    "validateParentRelationship should prevent self-parenting" {
        val scopeId = ScopeId.generate()
        val scope = Scope(
            id = scopeId,
            title = "Test Scope",
            description = null,
            parentId = null,
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now()
        )

        val result = ScopeValidationService.validateParentRelationship(
            scope = scope,
            newParentId = scopeId,
            allScopes = emptyList()
        )

        val error = result.shouldBeLeft()
        error shouldBe DomainError.ScopeError.SelfParenting
    }

    "validateParentRelationship should prevent circular reference" {
        val parentId = ScopeId.generate()
        val childId = ScopeId.generate()
        val grandChildId = ScopeId.generate()

        val parent = Scope(
            id = parentId,
            title = "Parent",
            description = null,
            parentId = grandChildId, // This creates the circular reference
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now()
        )

        val child = Scope(
            id = childId,
            title = "Child",
            description = null,
            parentId = parentId,
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now()
        )

        val grandChild = Scope(
            id = grandChildId,
            title = "Grand Child",
            description = null,
            parentId = childId,
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now()
        )

        val result = ScopeValidationService.validateParentRelationship(
            scope = grandChild,
            newParentId = childId,
            allScopes = listOf(parent, child, grandChild)
        )

        result.shouldBeLeft()
    }

    "validateHierarchyDepth should allow valid depth" {
        val result = ScopeValidationService.validateHierarchyDepth(
            parentId = null,
            allScopes = emptyList()
        )

        result.shouldBeRight()
    }

    "validateTitleUniqueness should allow unique titles" {
        val parentId = ScopeId.generate()
        val existingScope = Scope(
            id = ScopeId.generate(),
            title = "Existing Title",
            description = null,
            parentId = parentId,
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now()
        )

        val result = ScopeValidationService.validateTitleUniqueness(
            title = "New Title",
            parentId = parentId,
            excludeScopeId = null,
            allScopes = listOf(existingScope)
        )

        result.shouldBeRight()
    }

    "validateTitleUniqueness should prevent duplicate titles" {
        val parentId = ScopeId.generate()
        val existingScope = Scope(
            id = ScopeId.generate(),
            title = "Duplicate Title",
            description = null,
            parentId = parentId,
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now()
        )

        val result = ScopeValidationService.validateTitleUniqueness(
            title = "Duplicate Title",
            parentId = parentId,
            excludeScopeId = null,
            allScopes = listOf(existingScope)
        )

        val error = result.shouldBeLeft()
        error.shouldBeInstanceOf<DomainError.BusinessRuleViolation.DuplicateTitle>()
    }
})
