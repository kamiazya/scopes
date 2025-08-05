package io.github.kamiazya.scopes.domain.entity

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ScopeTest : StringSpec({

    "should create scope with valid data" {
        val scope = Scope.create(
            title = "Test Scope",
            description = "Test Description",
            parentId = null
        )

        val result = scope.shouldBeRight()
        result.title shouldBe "Test Scope"
        result.description shouldBe "Test Description"
        result.parentId shouldBe null
    }

    "should fail to create scope with empty title" {
        val scope = Scope.create(
            title = "",
            description = "Test Description",
            parentId = null
        )

        scope.shouldBeLeft()
    }

    "should fail to create scope with title too long" {
        val longTitle = "a".repeat(201)
        val scope = Scope.create(
            title = longTitle,
            description = "Test Description",
            parentId = null
        )

        scope.shouldBeLeft()
    }

    "should create scope with trimmed title" {
        val scope = Scope.create(
            title = "  Test Scope  ",
            description = "Test Description",
            parentId = null
        )

        val result = scope.shouldBeRight()
        result.title shouldBe "Test Scope"
    }

    "should create scope with null description when empty" {
        val scope = Scope.create(
            title = "Test Scope",
            description = "   ",
            parentId = null
        )

        val result = scope.shouldBeRight()
        result.description shouldBe null
    }
})
