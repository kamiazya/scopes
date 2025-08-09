package io.github.kamiazya.scopes.infrastructure.error

import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith

/**
 * Test for ErrorFormattingUtils.
 */
class ErrorFormattingUtilsTest : StringSpec({

    "ErrorFormattingUtils should exist in infrastructure layer" {
        // Test setup
        val utils = ErrorFormattingUtils

        // Should be able to access the object
        utils shouldBe ErrorFormattingUtils
    }

    "formatErrorMessages should create user-friendly summary for single error" {
        // Test setup
        val error = DomainError.ScopeValidationError.EmptyScopeTitle
        val errors = nonEmptyListOf(error)

        val result = ErrorFormattingUtils.formatErrorMessages(errors)

        result shouldStartWith "1 error found:"
        result shouldContain "ScopeValidationError (1)"
        result shouldContain "- Empty title"
    }

    "formatErrorMessages should create user-friendly summary for multiple errors" {
        // Test setup
        val error1 = DomainError.ScopeValidationError.EmptyScopeTitle
        val error2 = DomainError.ScopeValidationError.ScopeTitleTooShort
        val error3 = DomainError.ScopeValidationError.ScopeTitleTooLong(maxLength = 200, actualLength = 300)
        val errors = nonEmptyListOf(error1, error2, error3)

        val result = ErrorFormattingUtils.formatErrorMessages(errors)

        result shouldStartWith "3 errors found:"
        result shouldContain "ScopeValidationError (3)"
        result shouldContain "- Empty title"
        result shouldContain "- Title too short"
        result shouldContain "- Title too long"
    }

    "getErrorMessage should format validation errors properly" {
        // Test setup
        val emptyTitleError = DomainError.ScopeValidationError.EmptyScopeTitle
        val tooLongError = DomainError.ScopeValidationError.ScopeTitleTooLong(maxLength = 200, actualLength = 300)
        val newlineError = DomainError.ScopeValidationError.ScopeTitleContainsNewline

        ErrorFormattingUtils.getErrorMessage(emptyTitleError) shouldBe "Empty title"
        ErrorFormattingUtils.getErrorMessage(tooLongError) shouldBe "Title too long (max: 200, actual: 300)"
        ErrorFormattingUtils.getErrorMessage(newlineError) shouldBe "Title must not contain newline characters"
    }

    "getErrorMessage should format scope errors properly" {
        // Test setup
        val scopeNotFoundError = DomainError.ScopeError.ScopeNotFound
        val scopeId = ScopeId.generate()
        val parentId = ScopeId.generate()
        val circularRefError = DomainError.ScopeError.CircularReference(scopeId, parentId)
        val invalidTitleError = DomainError.ScopeError.InvalidTitle("too short")

        ErrorFormattingUtils.getErrorMessage(scopeNotFoundError) shouldBe "Scope not found"
        ErrorFormattingUtils.getErrorMessage(circularRefError) shouldContain "Circular reference detected between"
        ErrorFormattingUtils.getErrorMessage(circularRefError) shouldContain scopeId.value
        ErrorFormattingUtils.getErrorMessage(circularRefError) shouldContain parentId.value
        ErrorFormattingUtils.getErrorMessage(invalidTitleError) shouldBe "Invalid title: too short"
    }

    "getErrorMessage should format business rule violations properly" {
        // Test setup
        val maxDepthError = DomainError.ScopeBusinessRuleViolation.ScopeMaxDepthExceeded(maxDepth = 5, actualDepth = 8)
        val maxChildrenError = DomainError.ScopeBusinessRuleViolation
            .ScopeMaxChildrenExceeded(maxChildren = 10, actualChildren = 15)
        val duplicateError = DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle("Duplicate Task", null)

        ErrorFormattingUtils.getErrorMessage(maxDepthError) shouldBe
            "Maximum hierarchy depth exceeded (max: 5, actual: 8)"
        ErrorFormattingUtils.getErrorMessage(maxChildrenError) shouldBe
            "Maximum children exceeded (max: 10, actual: 15)"
        ErrorFormattingUtils.getErrorMessage(duplicateError) shouldBe "Duplicate title: Duplicate Task"
    }

    "getValidationErrorMessage should handle all validation error types" {
        // Test setup
        val emptyTitle = DomainError.ScopeValidationError.EmptyScopeTitle
        val tooShort = DomainError.ScopeValidationError.ScopeTitleTooShort
        val tooLong = DomainError.ScopeValidationError.ScopeTitleTooLong(200, 250)
        val newline = DomainError.ScopeValidationError.ScopeTitleContainsNewline
        val descTooLong = DomainError.ScopeValidationError.ScopeDescriptionTooLong(1000, 1500)
        val invalidFormat = DomainError.ScopeValidationError.ScopeInvalidFormat("title", "String")

        ErrorFormattingUtils.getValidationErrorMessage(emptyTitle) shouldBe "Empty title"
        ErrorFormattingUtils.getValidationErrorMessage(tooShort) shouldBe "Title too short"
        ErrorFormattingUtils.getValidationErrorMessage(tooLong) shouldBe "Title too long (max: 200, actual: 250)"
        ErrorFormattingUtils.getValidationErrorMessage(newline) shouldBe "Title must not contain newline characters"
        ErrorFormattingUtils.getValidationErrorMessage(descTooLong) shouldBe
            "Description too long (max: 1000, actual: 1500)"
        ErrorFormattingUtils.getValidationErrorMessage(invalidFormat) shouldBe
            "Invalid format for title, expected: String"
    }

    "getScopeErrorMessage should handle all scope error types" {
        // Test setup
        val notFound = DomainError.ScopeError.ScopeNotFound
        val selfParent = DomainError.ScopeError.SelfParenting
        val childId = ScopeId.generate()
        val parentId2 = ScopeId.generate()
        val parentId3 = ScopeId.generate()
        val circular = DomainError.ScopeError.CircularReference(childId, parentId2)
        val invalidTitle = DomainError.ScopeError.InvalidTitle("empty")
        val invalidDesc = DomainError.ScopeError.InvalidDescription("too long")
        val invalidParent = DomainError.ScopeError.InvalidParent(parentId3, "not found")

        ErrorFormattingUtils.getScopeErrorMessage(notFound) shouldBe "Scope not found"
        ErrorFormattingUtils.getScopeErrorMessage(selfParent) shouldBe "Scope cannot be its own parent"
        ErrorFormattingUtils.getScopeErrorMessage(circular) shouldContain "Circular reference detected between"
        ErrorFormattingUtils.getScopeErrorMessage(circular) shouldContain childId.value
        ErrorFormattingUtils.getScopeErrorMessage(circular) shouldContain parentId2.value
        ErrorFormattingUtils.getScopeErrorMessage(invalidTitle) shouldBe "Invalid title: empty"
        ErrorFormattingUtils.getScopeErrorMessage(invalidDesc) shouldBe "Invalid description: too long"
        ErrorFormattingUtils.getScopeErrorMessage(invalidParent) shouldContain "Invalid parent"
        ErrorFormattingUtils.getScopeErrorMessage(invalidParent) shouldContain parentId3.value
        ErrorFormattingUtils.getScopeErrorMessage(invalidParent) shouldContain "not found"
    }

    "getBusinessRuleViolationMessage should handle all business rule violations" {
        // Test setup
        val maxDepth = DomainError.ScopeBusinessRuleViolation.ScopeMaxDepthExceeded(10, 15)
        val maxChildren = DomainError.ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded(20, 25)
        val duplicate = DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle("Task Name", null)

        ErrorFormattingUtils.getBusinessRuleViolationMessage(maxDepth) shouldBe
            "Maximum hierarchy depth exceeded (max: 10, actual: 15)"
        ErrorFormattingUtils.getBusinessRuleViolationMessage(maxChildren) shouldBe
            "Maximum children exceeded (max: 20, actual: 25)"
        ErrorFormattingUtils.getBusinessRuleViolationMessage(duplicate) shouldBe "Duplicate title: Task Name"
    }
})
