package io.github.kamiazya.scopes.domain.entity

import io.github.kamiazya.scopes.domain.error.ScopeInputError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Basic tests to verify Scope validation works correctly.
 */
class BasicScopeTest : StringSpec({
    
    "valid title creates scope successfully" {
        val result = Scope.create(title = "Valid Title")
        result.shouldBeRight()
        result.getOrNull()?.title?.value shouldBe "Valid Title"
    }
    
    "empty title fails with Empty error" {
        val result = Scope.create(title = "")
        result.shouldBeLeft()
        val error = result.leftOrNull()
        error.shouldBeInstanceOf<ScopeInputError.TitleError.Empty>()
    }
    
    "title with newline fails with ContainsProhibitedCharacters error" {
        val result = Scope.create(title = "Title\nWith Newline")
        result.shouldBeLeft()
        val error = result.leftOrNull()
        error.shouldBeInstanceOf<ScopeInputError.TitleError.ContainsProhibitedCharacters>()
        if (error is ScopeInputError.TitleError.ContainsProhibitedCharacters) {
            error.prohibitedCharacters shouldBe listOf('\n', '\r')
        }
    }
    
    "very long title fails with TooLong error" {
        val longTitle = "a".repeat(201)
        val result = Scope.create(title = longTitle)
        result.shouldBeLeft()
        val error = result.leftOrNull()
        error.shouldBeInstanceOf<ScopeInputError.TitleError.TooLong>()
        if (error is ScopeInputError.TitleError.TooLong) {
            error.maximumLength shouldBe 200
        }
    }
    
    "valid description creates scope successfully" {
        val result = Scope.create(
            title = "Valid Title",
            description = "This is a valid description"
        )
        result.shouldBeRight()
        result.getOrNull()?.description?.value shouldBe "This is a valid description"
    }
    
    "empty description results in null" {
        val result = Scope.create(
            title = "Valid Title",
            description = ""
        )
        result.shouldBeRight()
        result.getOrNull()?.description shouldBe null
    }
    
    "very long description fails with TooLong error" {
        val longDescription = "a".repeat(1001)
        val result = Scope.create(
            title = "Valid Title",
            description = longDescription
        )
        result.shouldBeLeft()
        val error = result.leftOrNull()
        error.shouldBeInstanceOf<ScopeInputError.DescriptionError.TooLong>()
        if (error is ScopeInputError.DescriptionError.TooLong) {
            error.maximumLength shouldBe 1000
        }
    }
})
