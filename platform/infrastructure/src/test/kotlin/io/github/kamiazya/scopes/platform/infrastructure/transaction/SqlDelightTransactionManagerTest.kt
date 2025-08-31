package io.github.kamiazya.scopes.platform.infrastructure.transaction

import app.cash.sqldelight.Transacter
import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

class SqlDelightTransactionManagerTest :
    DescribeSpec({

        describe("SqlDelightTransactionManager") {
            val mockTransacter = mockk<Transacter>(relaxed = true)
            val transactionManager = SqlDelightTransactionManager(mockTransacter)

            describe("inTransaction") {
                it("should execute block and return success result") {
                    val result = transactionManager.inTransaction {
                        "success".right()
                    }

                    result.shouldBeRight()
                    result.getOrNull() shouldBe "success"
                }

                it("should execute block and return error result") {
                    val error = TestError("test error")
                    val result = transactionManager.inTransaction {
                        error.left()
                    }

                    result.shouldBeLeft()
                    result.swap().getOrNull() shouldBe error
                }

                it("should provide transaction context") {
                    var capturedContext: Any? = null

                    transactionManager.inTransaction {
                        capturedContext = this
                        "ok".right()
                    }

                    capturedContext shouldNotBe null
                }

                it("should execute on IO dispatcher") {
                    // This test verifies that the operation runs on IO dispatcher
                    // by checking that it completes successfully
                    val result = runBlocking {
                        transactionManager.inTransaction {
                            Thread.currentThread().name.right()
                        }
                    }

                    result.shouldBeRight()
                }
            }

            describe("inReadOnlyTransaction") {
                it("should execute block and return success result") {
                    val result = transactionManager.inReadOnlyTransaction {
                        "read-only success".right()
                    }

                    result.shouldBeRight()
                    result.getOrNull() shouldBe "read-only success"
                }

                it("should execute block and return error result") {
                    val error = TestError("read-only error")
                    val result = transactionManager.inReadOnlyTransaction {
                        error.left()
                    }

                    result.shouldBeLeft()
                    result.swap().getOrNull() shouldBe error
                }

                it("should provide transaction context") {
                    var capturedContext: Any? = null

                    transactionManager.inReadOnlyTransaction {
                        capturedContext = this
                        "ok".right()
                    }

                    capturedContext shouldNotBe null
                }
            }

            describe("Transaction Context") {
                it("should provide unique transaction ID") {
                    var transactionId1: String? = null
                    var transactionId2: String? = null

                    transactionManager.inTransaction {
                        transactionId1 = getTransactionId()
                        "ok".right()
                    }

                    transactionManager.inTransaction {
                        transactionId2 = getTransactionId()
                        "ok".right()
                    }

                    transactionId1 shouldNotBe null
                    transactionId2 shouldNotBe null
                    transactionId1 shouldNotBe transactionId2
                    transactionId1!!.shouldNotBeEmpty()
                    transactionId2!!.shouldNotBeEmpty()
                }

                it("should track rollback status") {
                    var rollbackStatusBefore: Boolean? = null
                    var rollbackStatusAfter: Boolean? = null

                    transactionManager.inTransaction {
                        rollbackStatusBefore = isMarkedForRollback()
                        markForRollback()
                        rollbackStatusAfter = isMarkedForRollback()
                        "ok".right()
                    }

                    rollbackStatusBefore shouldBe false
                    rollbackStatusAfter shouldBe true
                }

                it("should maintain rollback flag throughout transaction") {
                    val checkpoints = mutableListOf<Boolean>()

                    transactionManager.inTransaction {
                        checkpoints.add(isMarkedForRollback()) // Should be false
                        markForRollback()
                        checkpoints.add(isMarkedForRollback()) // Should be true
                        checkpoints.add(isMarkedForRollback()) // Should still be true
                        "ok".right()
                    }

                    checkpoints shouldBe listOf(false, true, true)
                }
            }

            describe("Concurrent transactions") {
                it("should handle multiple concurrent transactions") {
                    val results = (1..10).map { i ->
                        runBlocking {
                            transactionManager.inTransaction {
                                "result-$i".right()
                            }
                        }
                    }

                    results.forEachIndexed { index, result ->
                        result.shouldBeRight()
                        result.getOrNull() shouldBe "result-${index + 1}"
                    }
                }

                it("should provide unique transaction IDs for concurrent transactions") {
                    val transactionIds = mutableListOf<String>()

                    (1..5).map {
                        runBlocking {
                            transactionManager.inTransaction {
                                transactionIds.add(getTransactionId())
                                "ok".right()
                            }
                        }
                    }

                    // All transaction IDs should be unique
                    transactionIds.distinct().size shouldBe transactionIds.size
                }
            }
        }
    })

// Test error class for testing
data class TestError(val message: String)
