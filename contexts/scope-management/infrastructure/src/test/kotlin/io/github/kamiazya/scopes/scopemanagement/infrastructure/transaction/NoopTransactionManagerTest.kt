package io.github.kamiazya.scopes.scopemanagement.infrastructure.transaction

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

class NoopTransactionManagerTest :
    DescribeSpec({

        describe("NoopTransactionManager") {
            lateinit var transactionManager: NoopTransactionManager

            beforeEach {
                transactionManager = NoopTransactionManager()
            }

            describe("inTransaction") {
                it("should execute block and return success result") {
                    // Given
                    val expectedValue = "success"

                    // When
                    val result = transactionManager.inTransaction {
                        expectedValue.right()
                    }

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe expectedValue
                }

                it("should execute block and return error result") {
                    // Given
                    val expectedError = "error"

                    // When
                    val result = transactionManager.inTransaction {
                        expectedError.left()
                    }

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull() shouldBe expectedError
                }

                it("should handle suspend blocks") {
                    // Given
                    val expectedValue = "async-result"

                    // When
                    val result = transactionManager.inTransaction {
                        delay(1) // Simulate async operation
                        expectedValue.right()
                    }

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe expectedValue
                }

                it("should propagate exceptions from block") {
                    // Given
                    val expectedException = RuntimeException("test exception")

                    // When/Then
                    try {
                        transactionManager.inTransaction {
                            throw expectedException
                        }
                        throw AssertionError("Expected exception to be thrown")
                    } catch (e: RuntimeException) {
                        e shouldBe expectedException
                    }
                }

                it("should handle complex Either chains") {
                    // When
                    val result = transactionManager.inTransaction {
                        Either.catch {
                            // Simulate some complex operation
                            if (System.currentTimeMillis() > 0) {
                                "complex-operation-success"
                            } else {
                                throw RuntimeException("This should not happen")
                            }
                        }
                    }

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe "complex-operation-success"
                }

                it("should handle nested Either operations") {
                    // Given
                    data class TestError(val message: String)
                    data class TestValue(val data: String)

                    // When
                    val result = transactionManager.inTransaction {
                        val step1: Either<TestError, String> = "step1".right()
                        val step2: Either<TestError, String> = "step2".right()

                        step1.flatMap { s1 ->
                            step2.map { s2 ->
                                TestValue("$s1-$s2")
                            }
                        }
                    }

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe TestValue("step1-step2")
                }

                it("should handle error in nested Either operations") {
                    // Given
                    data class TestError(val message: String)

                    // When
                    val result = transactionManager.inTransaction {
                        val step1: Either<TestError, String> = "step1".right()
                        val step2: Either<TestError, String> = TestError("step2 failed").left()

                        step1.flatMap { s1 ->
                            step2.map { s2 ->
                                "$s1-$s2"
                            }
                        }
                    }

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull() shouldBe TestError("step2 failed")
                }

                it("should execute multiple operations in sequence") {
                    // Given
                    val operations = mutableListOf<String>()

                    // When
                    val result = transactionManager.inTransaction {
                        operations.add("operation1")
                        operations.add("operation2")
                        operations.add("operation3")
                        operations.joinToString(",").right()
                    }

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe "operation1,operation2,operation3"
                    operations shouldBe listOf("operation1", "operation2", "operation3")
                }

                it("should not provide any transaction guarantees") {
                    // This test documents that NoopTransactionManager provides no transaction semantics
                    // Given
                    val sideEffects = mutableListOf<String>()

                    // When - simulate a failure after side effects
                    val result = transactionManager.inTransaction {
                        sideEffects.add("side-effect-1")
                        sideEffects.add("side-effect-2")
                        "failure".left() // Return error after side effects
                    }

                    // Then - side effects remain (no rollback)
                    result.shouldBeLeft()
                    result.leftOrNull() shouldBe "failure"
                    sideEffects shouldBe listOf("side-effect-1", "side-effect-2") // Side effects persist
                }
            }

            describe("performance") {
                it("should have minimal overhead") {
                    // Given
                    val iterations = 1000

                    // When
                    val startTime = System.currentTimeMillis()
                    repeat(iterations) {
                        transactionManager.inTransaction {
                            "fast-operation".right()
                        }
                    }
                    val endTime = System.currentTimeMillis()

                    // Then - should complete quickly (no transaction overhead)
                    val duration = endTime - startTime
                    // This is a basic performance check - adjust threshold as needed
                    duration shouldBe { it < 1000 } // Should complete in less than 1 second
                }

                it("should handle concurrent access") {
                    // Given
                    val concurrentOperations = 10
                    val results = mutableListOf<Either<String, String>>()

                    // When
                    kotlinx.coroutines.runBlocking {
                        val jobs = (1..concurrentOperations).map { index ->
                            kotlinx.coroutines.async {
                                transactionManager.inTransaction {
                                    delay(10) // Small delay to simulate work
                                    "result-$index".right()
                                }
                            }
                        }
                        results.addAll(jobs.map { it.await() })
                    }

                    // Then
                    results.size shouldBe concurrentOperations
                    results.forEach { it.shouldBeRight() }
                    results.map { it.getOrNull() }.sorted() shouldBe
                        (1..concurrentOperations).map { "result-$it" }.sorted()
                }
            }

            describe("documentation compliance") {
                it("should fulfill the contract described in comments") {
                    // This test verifies the behavior matches the class documentation

                    // Given - block that should execute without transaction management
                    var blockExecuted = false
                    val expectedResult = "no-transaction-result"

                    // When
                    val result = transactionManager.inTransaction {
                        blockExecuted = true
                        expectedResult.right()
                    }

                    // Then - block executed and result returned as documented
                    blockExecuted shouldBe true
                    result.shouldBeRight()
                    result.getOrNull() shouldBe expectedResult
                }
            }
        }
    })
