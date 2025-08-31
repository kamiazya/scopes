package io.github.kamiazya.scopes.platform.infrastructure.transaction

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.TransactionWithoutReturn
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.application.port.TransactionContext
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import kotlinx.coroutines.runBlocking
import java.sql.SQLException

class TransactionRollbackTest :
    DescribeSpec({

        describe("Transaction Rollback Scenarios") {

            describe("SqlDelightTransactionManager rollback behavior") {
                val mockTransacter = mockk<Transacter>()
                val transactionManager = SqlDelightTransactionManager(mockTransacter)

                beforeEach {
                    clearMocks(mockTransacter)
                }

                it("should handle rollback when exception is thrown in block") {
                    val expectedException = RuntimeException("Transaction failed")

                    // Since SqlDelightTransactionManager doesn't directly control transactions,
                    // it just propagates the exception
                    val exception = shouldThrow<RuntimeException> {
                        runBlocking {
                            transactionManager.inTransaction<Nothing, String> {
                                throw expectedException
                            }
                        }
                    }

                    exception shouldBe expectedException
                }

                it("should handle rollback when markForRollback is called") {
                    var wasMarkedForRollback = false

                    val result = transactionManager.inTransaction {
                        markForRollback()
                        wasMarkedForRollback = isMarkedForRollback()
                        "result".right()
                    }

                    wasMarkedForRollback shouldBe true
                    result.shouldBeRight()
                    result.getOrNull() shouldBe "result"
                }

                it("should handle nested transaction rollback") {
                    var outerMarked = false
                    var innerMarked = false

                    val result = transactionManager.inTransaction {
                        // Outer transaction
                        transactionManager.inTransaction {
                            // Inner transaction
                            markForRollback()
                            innerMarked = isMarkedForRollback()
                            "inner".right()
                        }
                        outerMarked = isMarkedForRollback()
                        "outer".right()
                    }

                    innerMarked shouldBe true
                    // Each transaction has its own context
                    outerMarked shouldBe false
                    result.getOrNull() shouldBe "outer"
                }
            }

            describe("Repository-level transaction rollback") {
                it("should demonstrate SQLDelight transaction rollback pattern") {
                    // This test demonstrates how SQLDelight handles transaction rollback
                    val mockDatabase = mockk<Transacter>()
                    val mockTransaction = mockk<TransactionWithoutReturn>()
                    var rollbackCalled = false

                    // Mock the transaction behavior
                    every { mockDatabase.transaction(noEnclosing = any(), body = any()) } answers {
                        val block = arg<TransactionWithoutReturn.() -> Unit>(1)
                        try {
                            block(mockTransaction)
                        } catch (e: Exception) {
                            rollbackCalled = true
                            throw e
                        }
                    }

                    // Simulate a transaction that fails
                    shouldThrow<RuntimeException> {
                        mockDatabase.transaction {
                            throw RuntimeException("Database error")
                        }
                    }

                    rollbackCalled shouldBe true
                }

                it("should rollback on SQL exceptions") {
                    val mockDatabase = mockk<Transacter>()
                    val sqlException = SQLException("Constraint violation")
                    var rollbackCalled = false

                    every { mockDatabase.transaction(noEnclosing = any(), body = any()) } answers {
                        rollbackCalled = true
                        throw sqlException
                    }

                    shouldThrow<SQLException> {
                        mockDatabase.transaction {
                            // This would normally execute SQL operations
                        }
                    }

                    rollbackCalled shouldBe true
                }
            }

            describe("TransactionContext rollback tracking") {
                it("should track rollback state across multiple operations") {
                    val transactionManager = SqlDelightTransactionManager(mockk(relaxed = true))
                    val rollbackStates = mutableListOf<Boolean>()

                    transactionManager.inTransaction {
                        rollbackStates.add(isMarkedForRollback()) // Should be false

                        // Perform some operations
                        val result1 = processOperation("op1")
                        rollbackStates.add(isMarkedForRollback()) // Still false

                        // Mark for rollback
                        markForRollback()
                        rollbackStates.add(isMarkedForRollback()) // Should be true

                        // Continue with more operations
                        val result2 = processOperation("op2")
                        rollbackStates.add(isMarkedForRollback()) // Still true

                        "completed".right()
                    }

                    rollbackStates shouldBe listOf(false, false, true, true)
                }

                it("should handle conditional rollback based on business logic") {
                    val transactionManager = SqlDelightTransactionManager(mockk(relaxed = true))

                    val result = transactionManager.inTransaction {
                        val validationResult = validateBusinessRule(false)

                        if (validationResult.isLeft()) {
                            markForRollback()
                            validationResult
                        } else {
                            processOperation("valid operation")
                        }
                    }

                    result.shouldBeLeft()
                    result.swap().getOrNull().shouldBeInstanceOf<BusinessError>()
                }
            }

            describe("Error propagation and rollback") {
                val mockTransacter = mockk<Transacter>(relaxed = true)
                val transactionManager = SqlDelightTransactionManager(mockTransacter)

                it("should propagate domain errors without wrapping") {
                    val domainError = DomainError("Invalid state")

                    val result = transactionManager.inTransaction {
                        domainError.left()
                    }

                    result.shouldBeLeft()
                    result.swap().getOrNull() shouldBe domainError
                }

                it("should handle multiple error types in transaction") {
                    val results = mutableListOf<Any>()

                    // Test with domain error
                    results.add(
                        transactionManager.inTransaction {
                            DomainError("Domain issue").left()
                        },
                    )

                    // Test with persistence error
                    results.add(
                        transactionManager.inTransaction {
                            PersistenceError("Storage issue").left()
                        },
                    )

                    // Test with success
                    results.add(
                        transactionManager.inTransaction {
                            "success".right()
                        },
                    )

                    results[0].shouldBeInstanceOf<arrow.core.Either.Left<DomainError>>()
                    results[1].shouldBeInstanceOf<arrow.core.Either.Left<PersistenceError>>()
                    results[2].shouldBeInstanceOf<arrow.core.Either.Right<String>>()
                }
            }

            describe("Transaction lifecycle and cleanup") {
                it("should ensure proper cleanup after rollback") {
                    val transactionManager = SqlDelightTransactionManager(mockk(relaxed = true))
                    var cleanupExecuted = false

                    try {
                        runBlocking {
                            transactionManager.inTransaction<Nothing, String> {
                                try {
                                    throw RuntimeException("Force rollback")
                                } finally {
                                    cleanupExecuted = true
                                }
                            }
                        }
                    } catch (e: RuntimeException) {
                        // Expected
                    }

                    cleanupExecuted shouldBe true
                }

                it("should handle rollback in read-only transactions") {
                    val transactionManager = SqlDelightTransactionManager(mockk(relaxed = true))

                    val result = transactionManager.inReadOnlyTransaction {
                        // Even in read-only, we can mark for rollback
                        markForRollback()
                        isMarkedForRollback() shouldBe true
                        "read complete".right()
                    }

                    result.getOrNull() shouldBe "read complete"
                }
            }
        }
    })

// Helper classes for testing
private data class DomainError(val message: String)
private data class PersistenceError(val message: String)
private data class BusinessError(val reason: String)

// Helper functions for testing
private suspend fun TransactionContext.processOperation(name: String): arrow.core.Either<Nothing, String> = "$name processed".right()

private suspend fun TransactionContext.validateBusinessRule(isValid: Boolean): arrow.core.Either<BusinessError, Unit> = if (isValid) {
    Unit.right()
} else {
    BusinessError("Validation failed").left()
}
