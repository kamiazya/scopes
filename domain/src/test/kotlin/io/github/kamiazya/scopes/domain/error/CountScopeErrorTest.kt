package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.seconds

/**
 * Test cases for CountScopeError sealed class.
 * These tests define the expected behavior for count operation failures.
 */
class CountScopeErrorTest : FunSpec({

    context("CountScopeError sealed class") {

        test("should create AggregationTimeout error with timeout duration") {
            val timeout = 10.seconds
            val error = CountScopeError.AggregationTimeout(timeout)

            error.shouldBeInstanceOf<CountScopeError.AggregationTimeout>()
            error.timeout shouldBe timeout
        }

        test("should create ConnectionError with parent ID and retryable flag") {
            val parentId = ScopeId.generate()
            val cause = RuntimeException("Connection reset")
            val error = CountScopeError.ConnectionError(parentId, retryable = true, cause = cause)

            error.shouldBeInstanceOf<CountScopeError.ConnectionError>()
            error.parentId shouldBe parentId
            error.retryable shouldBe true
            error.cause shouldBe cause
        }

        test("should create InvalidParentId error with parent ID and message") {
            val parentId = ScopeId.generate()
            val message = "Parent scope does not exist"
            val error = CountScopeError.InvalidParentId(parentId, message)

            error.shouldBeInstanceOf<CountScopeError.InvalidParentId>()
            error.parentId shouldBe parentId
            error.message shouldBe message
        }

        test("should create PersistenceError error with parent ID, message and cause") {
            val parentId = ScopeId.generate()
            val message = "Storage system unavailable"
            val cause = RuntimeException("Disk full")
            val error = CountScopeError.PersistenceError(parentId, message, cause)

            error.shouldBeInstanceOf<CountScopeError.PersistenceError>()
            error.parentId shouldBe parentId
            error.message shouldBe message
            error.cause shouldBe cause
        }

        test("should create UnknownError error with parent ID, message and cause") {
            val parentId = ScopeId.generate()
            val message = "Unexpected error during count operation"
            val cause = RuntimeException("Unknown failure")
            val error = CountScopeError.UnknownError(parentId, message, cause)

            error.shouldBeInstanceOf<CountScopeError.UnknownError>()
            error.parentId shouldBe parentId
            error.message shouldBe message
            error.cause shouldBe cause
        }
    }

    context("CountScopeError sealed hierarchy") {

        test("all error types should be instances of CountScopeError") {
            val parentId = ScopeId.generate()
            val cause = RuntimeException("Test")

            val errors = listOf(
                CountScopeError.AggregationTimeout(10.seconds),
                CountScopeError.ConnectionError(parentId, retryable = true, cause = cause),
                CountScopeError.InvalidParentId(parentId, "test"),
                CountScopeError.PersistenceError(parentId, "test", cause),
                CountScopeError.UnknownError(parentId, "test", cause)
            )

            errors.forEach { error ->
                error.shouldBeInstanceOf<CountScopeError>()
            }
        }
    }
})
