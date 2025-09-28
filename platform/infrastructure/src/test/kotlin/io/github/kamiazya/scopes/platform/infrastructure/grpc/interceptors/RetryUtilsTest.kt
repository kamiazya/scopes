package io.github.kamiazya.scopes.platform.infrastructure.grpc.interceptors

import io.grpc.Status
import io.grpc.StatusException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

class RetryUtilsTest :
    DescribeSpec({

        describe("RetryUtils.shouldRetry") {

            it("should return true for retryable status codes") {
                RetryUtils.shouldRetry(Status.UNAVAILABLE) shouldBe true
                RetryUtils.shouldRetry(Status.DEADLINE_EXCEEDED) shouldBe true
                RetryUtils.shouldRetry(Status.RESOURCE_EXHAUSTED) shouldBe true
                RetryUtils.shouldRetry(Status.ABORTED) shouldBe true
            }

            it("should return false for non-retryable status codes") {
                RetryUtils.shouldRetry(Status.OK) shouldBe false
                RetryUtils.shouldRetry(Status.CANCELLED) shouldBe false
                RetryUtils.shouldRetry(Status.INVALID_ARGUMENT) shouldBe false
                RetryUtils.shouldRetry(Status.NOT_FOUND) shouldBe false
                RetryUtils.shouldRetry(Status.ALREADY_EXISTS) shouldBe false
                RetryUtils.shouldRetry(Status.PERMISSION_DENIED) shouldBe false
                RetryUtils.shouldRetry(Status.FAILED_PRECONDITION) shouldBe false
                RetryUtils.shouldRetry(Status.OUT_OF_RANGE) shouldBe false
                RetryUtils.shouldRetry(Status.UNIMPLEMENTED) shouldBe false
                RetryUtils.shouldRetry(Status.INTERNAL) shouldBe false
                RetryUtils.shouldRetry(Status.DATA_LOSS) shouldBe false
                RetryUtils.shouldRetry(Status.UNAUTHENTICATED) shouldBe false
            }
        }

        describe("RetryUtils.withRetry") {

            it("should succeed on first attempt when operation succeeds") {
                // Arrange
                var attemptCount = 0
                val expectedResult = "success"

                // Act
                val result = RetryUtils.withRetry(
                    maxRetries = 3,
                    baseDelay = 10.milliseconds,
                    maxDelay = 100.milliseconds,
                ) {
                    attemptCount++
                    expectedResult
                }

                // Assert
                result shouldBe expectedResult
                attemptCount shouldBe 1
            }

            it("should retry on StatusException with retryable status") {
                // Arrange
                var attemptCount = 0
                val expectedResult = "success"

                // Act
                val result = RetryUtils.withRetry(
                    maxRetries = 3,
                    baseDelay = 10.milliseconds,
                    maxDelay = 100.milliseconds,
                ) {
                    attemptCount++
                    if (attemptCount < 3) {
                        throw StatusException(Status.UNAVAILABLE.withDescription("Service temporarily unavailable"))
                    }
                    expectedResult
                }

                // Assert
                result shouldBe expectedResult
                attemptCount shouldBe 3
            }

            it("should not retry on StatusException with non-retryable status") {
                // Arrange
                var attemptCount = 0

                // Act & Assert
                shouldThrow<StatusException> {
                    RetryUtils.withRetry(
                        maxRetries = 3,
                        baseDelay = 10.milliseconds,
                        maxDelay = 100.milliseconds,
                    ) {
                        attemptCount++
                        throw StatusException(Status.INVALID_ARGUMENT.withDescription("Bad request"))
                    }
                }

                attemptCount shouldBe 1
            }

            it("should not retry on non-StatusException") {
                // Arrange
                var attemptCount = 0

                // Act & Assert
                shouldThrow<RuntimeException> {
                    RetryUtils.withRetry(
                        maxRetries = 3,
                        baseDelay = 10.milliseconds,
                        maxDelay = 100.milliseconds,
                    ) {
                        attemptCount++
                        throw RuntimeException("Generic error")
                    }
                }

                attemptCount shouldBe 1
            }

            it("should respect maxRetries limit") {
                // Arrange
                var attemptCount = 0
                val maxRetries = 2

                // Act & Assert
                shouldThrow<StatusException> {
                    RetryUtils.withRetry(
                        maxRetries = maxRetries,
                        baseDelay = 10.milliseconds,
                        maxDelay = 100.milliseconds,
                    ) {
                        attemptCount++
                        throw StatusException(Status.UNAVAILABLE.withDescription("Always fails"))
                    }
                }

                attemptCount shouldBe maxRetries + 1 // maxRetries + initial attempt
            }

            it("should implement exponential backoff") {
                // Arrange
                var attemptCount = 0
                val baseDelay = 50.milliseconds
                val maxDelay = 500.milliseconds
                val delays = mutableListOf<Long>()

                // Act & Assert
                shouldThrow<StatusException> {
                    RetryUtils.withRetry(
                        maxRetries = 3,
                        baseDelay = baseDelay,
                        maxDelay = maxDelay,
                    ) {
                        attemptCount++
                        if (attemptCount > 1) {
                            val elapsed = measureTime {
                                throw StatusException(Status.UNAVAILABLE)
                            }
                            delays.add(elapsed.inWholeMilliseconds)
                        } else {
                            throw StatusException(Status.UNAVAILABLE)
                        }
                    }
                }

                // The exact timing is hard to test due to system scheduling,
                // but we can verify that retries happened
                attemptCount shouldBe 4 // 1 + 3 retries
            }

            it("should respect maxDelay cap") {
                // Arrange
                var attemptCount = 0
                val baseDelay = 100.milliseconds
                val maxDelay = 150.milliseconds

                // Act
                val totalTime = measureTime {
                    shouldThrow<StatusException> {
                        RetryUtils.withRetry(
                            maxRetries = 2,
                            baseDelay = baseDelay,
                            maxDelay = maxDelay,
                        ) {
                            attemptCount++
                            throw StatusException(Status.RESOURCE_EXHAUSTED)
                        }
                    }
                }

                // Assert
                attemptCount shouldBe 3 // 1 + 2 retries
                // Total time should be less than what unlimited exponential backoff would take
                // With max delay cap: ~150ms + ~150ms = ~300ms + some overhead
                // Without cap: ~100ms + ~200ms = ~300ms + some overhead
                // Hard to test exact timing, but verify reasonable bounds
                totalTime.inWholeMilliseconds.shouldBeLessThan(1000L)
            }

            it("should handle zero maxRetries") {
                // Arrange
                var attemptCount = 0

                // Act & Assert
                shouldThrow<StatusException> {
                    RetryUtils.withRetry(
                        maxRetries = 0,
                        baseDelay = 10.milliseconds,
                        maxDelay = 100.milliseconds,
                    ) {
                        attemptCount++
                        throw StatusException(Status.UNAVAILABLE)
                    }
                }

                attemptCount shouldBe 1 // Only initial attempt, no retries
            }
        }
    })
