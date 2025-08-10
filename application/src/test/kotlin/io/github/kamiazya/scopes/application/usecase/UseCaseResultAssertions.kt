package io.github.kamiazya.scopes.application.usecase

import io.github.kamiazya.scopes.application.error.ApplicationError
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Test assertions for UseCaseResult to replace Arrow Either assertions.
 * Provides similar API to kotest-assertions-arrow for consistent testing.
 */

/**
 * Assert that a UseCaseResult is Ok and return the success value.
 * Equivalent to shouldBeRight() for Either.
 */
fun <T> UseCaseResult<T>.shouldBeOk(): T = when (this) {
    is UseCaseResult.Ok -> value
    is UseCaseResult.Err -> throw AssertionError("Expected Ok but was Err with error: $error")
}

/**
 * Assert that a UseCaseResult is Ok with the expected value.
 */
fun <T> UseCaseResult<T>.shouldBeOk(expected: T): T {
    val actual = shouldBeOk()
    actual shouldBe expected
    return actual
}

/**
 * Assert that a UseCaseResult is Err and return the error.
 * Equivalent to shouldBeLeft() for Either.
 */
fun <T> UseCaseResult<T>.shouldBeErr(): ApplicationError = when (this) {
    is UseCaseResult.Ok -> throw AssertionError("Expected Err but was Ok with value: $value")
    is UseCaseResult.Err -> error
}

/**
 * Assert that a UseCaseResult is Err with the expected error.
 */
fun <T> UseCaseResult<T>.shouldBeErr(expected: ApplicationError): ApplicationError {
    val actual = shouldBeErr()
    actual shouldBe expected
    return actual
}

/**
 * Assert that a UseCaseResult is Ok and apply additional assertions to the value.
 */
inline fun <T> UseCaseResult<T>.shouldBeOkAnd(block: (T) -> Unit): T {
    val value = shouldBeOk()
    block(value)
    return value
}

/**
 * Assert that a UseCaseResult is Err and apply additional assertions to the error.
 */
inline fun <T> UseCaseResult<T>.shouldBeErrAnd(block: (ApplicationError) -> Unit): ApplicationError {
    val error = shouldBeErr()
    block(error)
    return error
}

/**
 * Assert that a UseCaseResult is Err with an error of the expected type.
 */
inline fun <reified E : ApplicationError> UseCaseResult<*>.shouldBeErrOfType(): E {
    val error = shouldBeErr()
    return error.shouldBeInstanceOf<E>()
}
