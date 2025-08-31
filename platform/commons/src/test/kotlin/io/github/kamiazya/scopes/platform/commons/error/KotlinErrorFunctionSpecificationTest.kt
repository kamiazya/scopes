package io.github.kamiazya.scopes.platform.commons.error

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Specification tests for Kotlin error function usage.
 *
 * Documents the project-wide decision to use Kotlin's standard
 * error functions instead of throwing exceptions directly.
 */
class KotlinErrorFunctionSpecificationTest :
    DescribeSpec({

        describe("Kotlin Error Functions Specification") {

            describe("error() function usage") {
                it("should use error() for illegal states") {
                    /**
                     * SPECIFICATION: Use error() for "should never happen" states.
                     *
                     * The error() function is used when:
                     * - The code reaches an illegal state
                     * - Data integrity is compromised
                     * - An unmapped case is encountered
                     *
                     * Example:
                     * ```kotlin
                     * when (type) {
                     *     Type.A -> handleA()
                     *     Type.B -> handleB()
                     *     else -> error("Unmapped type: $type")
                     * }
                     * ```
                     */

                    val exception = shouldThrow<IllegalStateException> {
                        error("This should never happen")
                    }

                    exception.message shouldBe "This should never happen"
                    exception.shouldBeInstanceOf<IllegalStateException>()
                }

                it("should prefer error() over throw IllegalStateException") {
                    /**
                     * SPECIFICATION: Kotlin idiomatic style.
                     *
                     * Project style guide:
                     * ❌ BAD:  throw IllegalStateException("message")
                     * ✅ GOOD: error("message")
                     *
                     * This is enforced by Konsist architecture tests.
                     */

                    // Both achieve the same result, but error() is preferred
                    val errorException = shouldThrow<IllegalStateException> {
                        error("Using error function")
                    }

                    val throwException = shouldThrow<IllegalStateException> {
                        throw IllegalStateException("Using throw directly")
                    }

                    // Same exception type, but error() is more concise
                    errorException::class shouldBe throwException::class
                }
            }

            describe("require() function usage") {
                it("should use require() for precondition validation") {
                    /**
                     * SPECIFICATION: Use require() for argument validation.
                     *
                     * The require() function validates preconditions:
                     * - Function arguments
                     * - Constructor parameters
                     * - Method preconditions
                     *
                     * Example:
                     * ```kotlin
                     * fun process(items: List<String>) {
                     *     require(items.isNotEmpty()) { "Items cannot be empty" }
                     *     // process items
                     * }
                     * ```
                     */

                    val exception = shouldThrow<IllegalArgumentException> {
                        require(false) { "Precondition failed" }
                    }

                    exception.message shouldBe "Precondition failed"
                    exception.shouldBeInstanceOf<IllegalArgumentException>()
                }

                it("should use require() with clear error messages") {
                    /**
                     * SPECIFICATION: Error messages must be descriptive.
                     *
                     * Good:
                     * ```kotlin
                     * require(age >= 0) { "Age cannot be negative: $age" }
                     * ```
                     *
                     * Bad:
                     * ```kotlin
                     * require(age >= 0) { "Invalid" }
                     * ```
                     */

                    val invalidAge = -5
                    val exception = shouldThrow<IllegalArgumentException> {
                        require(invalidAge >= 0) { "Age cannot be negative: $invalidAge" }
                    }

                    exception.message shouldContain "Age cannot be negative"
                    exception.message shouldContain "-5"
                }
            }

            describe("check() function usage") {
                it("should use check() for postcondition validation") {
                    /**
                     * SPECIFICATION: Use check() to verify state after operations.
                     *
                     * The check() function validates postconditions:
                     * - Operation results
                     * - State invariants
                     * - Method postconditions
                     *
                     * Example:
                     * ```kotlin
                     * fun withdraw(amount: Int): Int {
                     *     balance -= amount
                     *     check(balance >= 0) { "Balance cannot be negative" }
                     *     return balance
                     * }
                     * ```
                     */

                    val exception = shouldThrow<IllegalStateException> {
                        check(false) { "Postcondition failed" }
                    }

                    exception.message shouldBe "Postcondition failed"
                    exception.shouldBeInstanceOf<IllegalStateException>()
                }
            }

            describe("checkNotNull() function usage") {
                it("should use checkNotNull() for null safety with context") {
                    /**
                     * SPECIFICATION: Use checkNotNull() when null is unexpected.
                     *
                     * Better than !! operator because it provides context:
                     * ```kotlin
                     * // BAD
                     * val value = nullable!!
                     *
                     * // GOOD
                     * val value = checkNotNull(nullable) {
                     *     "Expected non-null value but was null"
                     * }
                     * ```
                     */

                    val nullable: String? = null

                    val exception = shouldThrow<IllegalStateException> {
                        checkNotNull(nullable) { "Value was unexpectedly null" }
                    }

                    exception.message shouldBe "Value was unexpectedly null"
                }
            }

            describe("Function selection guide") {
                it("should document when to use each function") {
                    /**
                     * SPECIFICATION: Function selection matrix.
                     *
                     * | Scenario | Function | Exception Type |
                     * |----------|----------|----------------|
                     * | Invalid argument | require() | IllegalArgumentException |
                     * | Invalid state | error() or check() | IllegalStateException |
                     * | Unexpected null | checkNotNull() | IllegalStateException |
                     * | Not implemented | TODO() | NotImplementedError |
                     * | Precondition | require() | IllegalArgumentException |
                     * | Postcondition | check() | IllegalStateException |
                     * | Data corruption | error() | IllegalStateException |
                     *
                     * CLI Layer Exception:
                     * - CLI commands may use `throw CliktError()` for user-facing errors
                     */

                    true shouldBe true
                }
            }

            describe("Project conventions") {
                it("should document that this is enforced by Konsist") {
                    /**
                     * SPECIFICATION: Automated enforcement.
                     *
                     * The Konsist architecture tests enforce:
                     * 1. No direct `throw IllegalStateException`
                     * 2. No direct `throw IllegalArgumentException`
                     * 3. Use of Kotlin standard functions instead
                     *
                     * This ensures consistency across the codebase.
                     */

                    true shouldBe true
                }
            }
        }
    })
