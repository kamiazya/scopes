package io.github.kamiazya.scopes.application.service

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.domain.error.DomainInfrastructureError
import io.github.kamiazya.scopes.domain.error.ExistsScopeError
import io.github.kamiazya.scopes.domain.error.RepositoryError
import io.github.kamiazya.scopes.domain.error.ScopeError
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.beInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import java.sql.SQLException
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive test for ExistsScopeError to ApplicationValidationError mapping.
 *
 * This test suite validates the error mapping logic in ApplicationScopeValidationService,
 * specifically focusing on how different ExistsScopeError variants are converted to
 * appropriate DomainError instances during title uniqueness validation.
 *
 * Coverage includes:
 * - All ExistsScopeError variants with their specific fields
 * - Error conversion with and without null causes
 * - Proper field mapping for timeout, retryable, errorCode, message, context
 */
class ApplicationScopeValidationServiceErrorMappingTest : DescribeSpec({

    val mockScopeRepository = mockk<ScopeRepository>()
    val service = ApplicationScopeValidationService(mockScopeRepository)

    describe("ExistsScopeError to DomainError mapping") {

        describe("IndexCorruption error mapping") {
            it("should map IndexCorruption with parentId to ScopeError.InvalidParent") {
                // Given
                val parentId = ScopeId.generate()
                val title = "Test Title"
                val normalizedTitle = title.lowercase().trim()
                val corruptionMessage = "Index corruption detected in scope_title_idx"
                val corruptedScopeId = ScopeId.generate()

                val indexCorruptionError = ExistsScopeError.IndexCorruption(
                    scopeId = corruptedScopeId,
                    message = corruptionMessage
                )

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(parentId, normalizedTitle)
                } returns indexCorruptionError.left()

                // When
                val result = service.validateTitleUniqueness(title, parentId)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<ScopeError.InvalidParent>()

                val invalidParentError = error as ScopeError.InvalidParent
                invalidParentError.parentId shouldBe parentId
                invalidParentError.reason shouldContain "Index corruption detected for parent"
                invalidParentError.reason shouldContain corruptionMessage
                invalidParentError.reason shouldContain corruptedScopeId.toString()
            }

            it("should map IndexCorruption without parentId to DomainInfrastructureError") {
                // Given
                val title = "Test Title"
                val normalizedTitle = title.lowercase().trim()
                val corruptionMessage = "Root level index corruption"
                val corruptedScopeId = ScopeId.generate()

                val indexCorruptionError = ExistsScopeError.IndexCorruption(
                    scopeId = corruptedScopeId,
                    message = corruptionMessage
                )

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(null, normalizedTitle)
                } returns indexCorruptionError.left()

                // When
                val result = service.validateTitleUniqueness(title, null)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<DomainInfrastructureError>()

                val infrastructureError = error as DomainInfrastructureError
                infrastructureError.repositoryError should beInstanceOf<RepositoryError.DataIntegrityError>()

                val dataIntegrityError = infrastructureError.repositoryError as RepositoryError.DataIntegrityError
                dataIntegrityError.message shouldContain "Index corruption detected for root-level existence check"
                dataIntegrityError.message shouldContain corruptionMessage
                dataIntegrityError.message shouldContain corruptedScopeId.toString()
                dataIntegrityError.cause should beInstanceOf<RuntimeException>()
            }

            it("should handle IndexCorruption with null scopeId") {
                // Given
                val parentId = ScopeId.generate()
                val title = "Test Title"
                val normalizedTitle = title.lowercase().trim()
                val corruptionMessage = "Scope ID is null due to corruption"

                val indexCorruptionError = ExistsScopeError.IndexCorruption(
                    scopeId = null,
                    message = corruptionMessage
                )

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(parentId, normalizedTitle)
                } returns indexCorruptionError.left()

                // When
                val result = service.validateTitleUniqueness(title, parentId)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<ScopeError.InvalidParent>()

                val invalidParentError = error as ScopeError.InvalidParent
                invalidParentError.reason shouldContain "Index corruption detected for parent"
                invalidParentError.reason shouldContain corruptionMessage
                invalidParentError.reason shouldContain "ScopeId in corruption: null"
            }
        }

        describe("QueryTimeout error mapping") {
            it("should map QueryTimeout to DomainInfrastructureError with RepositoryError.DatabaseError") {
                // Given
                val parentId = ScopeId.generate()
                val title = "Test Title"
                val normalizedTitle = title.lowercase().trim()
                val operation = "EXISTS_BY_PARENT_AND_TITLE"
                val timeout = 5.seconds
                val context = ExistsScopeError.ExistenceContext.ByParentIdAndTitle(parentId, title)

                val queryTimeoutError = ExistsScopeError.QueryTimeout(
                    context = context,
                    timeout = timeout,
                    operation = operation
                )

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(parentId, normalizedTitle)
                } returns queryTimeoutError.left()

                // When
                val result = service.validateTitleUniqueness(title, parentId)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<DomainInfrastructureError>()

                val infrastructureError = error as DomainInfrastructureError
                infrastructureError.repositoryError should beInstanceOf<RepositoryError.DatabaseError>()

                val databaseError = infrastructureError.repositoryError as RepositoryError.DatabaseError
                databaseError.message shouldContain "Query timeout during existence check"
                databaseError.message shouldContain "operation='$operation'"
                databaseError.message shouldContain "timeout=${timeout.inWholeMilliseconds}ms"
                databaseError.message shouldContain "context=$context"
                databaseError.cause should beInstanceOf<RuntimeException>()
                databaseError.cause!!.message shouldBe "Query timeout: $operation"
            }

            it("should map QueryTimeout with ById context") {
                // Given
                val scopeId = ScopeId.generate()
                val title = "Test Title"
                val normalizedTitle = title.lowercase().trim()
                val operation = "EXISTS_BY_ID"
                val timeout = 3.seconds
                val context = ExistsScopeError.ExistenceContext.ById(scopeId)

                val queryTimeoutError = ExistsScopeError.QueryTimeout(
                    context = context,
                    timeout = timeout,
                    operation = operation
                )

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(null, normalizedTitle)
                } returns queryTimeoutError.left()

                // When
                val result = service.validateTitleUniqueness(title, null)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<DomainInfrastructureError>()

                val infrastructureError = error as DomainInfrastructureError
                val databaseError = infrastructureError.repositoryError as RepositoryError.DatabaseError
                databaseError.message shouldContain "operation='$operation'"
                databaseError.message shouldContain "timeout=${timeout.inWholeMilliseconds}ms"
                databaseError.message shouldContain "context=$context"
            }
        }

        describe("LockTimeout error mapping") {
            it("should map LockTimeout with retryable=true to DomainInfrastructureError") {
                // Given
                val parentId = ScopeId.generate()
                val title = "Test Title"
                val normalizedTitle = title.lowercase().trim()
                val operation = "ACQUIRE_TABLE_LOCK"
                val timeout = 10.seconds
                val retryable = true

                val lockTimeoutError = ExistsScopeError.LockTimeout(
                    timeout = timeout,
                    operation = operation,
                    retryable = retryable
                )

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(parentId, normalizedTitle)
                } returns lockTimeoutError.left()

                // When
                val result = service.validateTitleUniqueness(title, parentId)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<DomainInfrastructureError>()

                val infrastructureError = error as DomainInfrastructureError
                infrastructureError.repositoryError should beInstanceOf<RepositoryError.DatabaseError>()

                val databaseError = infrastructureError.repositoryError as RepositoryError.DatabaseError
                databaseError.message shouldContain "Lock timeout during existence check"
                databaseError.message shouldContain "operation='$operation'"
                databaseError.message shouldContain "timeout=${timeout.inWholeMilliseconds}ms"
                databaseError.message shouldContain "retryable=$retryable"
                databaseError.cause should beInstanceOf<RuntimeException>()
                databaseError.cause!!.message shouldBe "Lock timeout: $operation"
            }

            it("should map LockTimeout with retryable=false") {
                // Given
                val title = "Test Title"
                val normalizedTitle = title.lowercase().trim()
                val operation = "DEADLOCK_DETECTED"
                val timeout = 15.seconds
                val retryable = false

                val lockTimeoutError = ExistsScopeError.LockTimeout(
                    timeout = timeout,
                    operation = operation,
                    retryable = retryable
                )

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(null, normalizedTitle)
                } returns lockTimeoutError.left()

                // When
                val result = service.validateTitleUniqueness(title, null)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<DomainInfrastructureError>()

                val infrastructureError = error as DomainInfrastructureError
                val databaseError = infrastructureError.repositoryError as RepositoryError.DatabaseError
                databaseError.message shouldContain "retryable=$retryable"
            }
        }

        describe("ConnectionFailure error mapping") {
            it("should map ConnectionFailure to DomainInfrastructureError with RepositoryError.ConnectionError") {
                // Given
                val parentId = ScopeId.generate()
                val title = "Test Title"
                val normalizedTitle = title.lowercase().trim()
                val connectionMessage = "Connection refused: database server unavailable"
                val cause = SQLException("Connection timed out")

                val connectionFailureError = ExistsScopeError.ConnectionFailure(
                    message = connectionMessage,
                    cause = cause
                )

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(parentId, normalizedTitle)
                } returns connectionFailureError.left()

                // When
                val result = service.validateTitleUniqueness(title, parentId)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<DomainInfrastructureError>()

                val infrastructureError = error as DomainInfrastructureError
                infrastructureError.repositoryError should beInstanceOf<RepositoryError.ConnectionError>()

                val connectionError = infrastructureError.repositoryError as RepositoryError.ConnectionError
                connectionError.cause shouldBe cause
            }

            it("should handle ConnectionFailure with null cause") {
                // Given
                val title = "Test Title"
                val normalizedTitle = title.lowercase().trim()
                val connectionMessage = "Network connection lost"

                val connectionFailureError = ExistsScopeError.ConnectionFailure(
                    message = connectionMessage,
                    cause = RuntimeException("Test exception with null cause", null)
                )

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(null, normalizedTitle)
                } returns connectionFailureError.left()

                // When
                val result = service.validateTitleUniqueness(title, null)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<DomainInfrastructureError>()

                val infrastructureError = error as DomainInfrastructureError
                val connectionError = infrastructureError.repositoryError as RepositoryError.ConnectionError

                // Verify no NullPointerException when accessing cause?.message
                val causeMessage = connectionError.cause.cause?.message
                causeMessage shouldBe null  // Should be null, not throw NPE
            }
        }

        describe("PersistenceError error mapping") {
            it("should map PersistenceError with all fields to DomainInfrastructureError") {
                // Given
                val parentId = ScopeId.generate()
                val title = "Test Title"
                val normalizedTitle = title.lowercase().trim()
                val context = ExistsScopeError.ExistenceContext.ByParentIdAndTitle(parentId, title)
                val message = "Persistence layer failed during existence check"
                val cause = SQLException("Table lock timeout")
                val retryable = true
                val errorCode = "PERSISTENCE_001"
                val category = ExistsScopeError.PersistenceError.ErrorCategory.DATABASE

                val persistenceError = ExistsScopeError.PersistenceError(
                    context = context,
                    message = message,
                    cause = cause,
                    retryable = retryable,
                    errorCode = errorCode,
                    category = category
                )

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(parentId, normalizedTitle)
                } returns persistenceError.left()

                // When
                val result = service.validateTitleUniqueness(title, parentId)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<DomainInfrastructureError>()

                val infrastructureError = error as DomainInfrastructureError
                infrastructureError.repositoryError should beInstanceOf<RepositoryError.DatabaseError>()

                val databaseError = infrastructureError.repositoryError as RepositoryError.DatabaseError
                databaseError.message shouldContain "Persistence error during existence check"
                databaseError.message shouldContain "context=$context"
                databaseError.message shouldContain "retryable=$retryable"
                databaseError.message shouldContain "errorCode=$errorCode"
                databaseError.message shouldContain message
                databaseError.cause shouldBe cause
            }

            it("should map PersistenceError with null errorCode") {
                // Given
                val title = "Test Title"
                val normalizedTitle = title.lowercase().trim()
                val context = ExistsScopeError.ExistenceContext.ByCustomCriteria(
                    mapOf("filter" to "custom", "type" to "existence")
                )
                val message = "Custom criteria existence check failed"
                val cause = RuntimeException("Database unavailable")
                val retryable = false
                val errorCode = null  // Testing null errorCode
                val category = ExistsScopeError.PersistenceError.ErrorCategory.CONNECTION

                val persistenceError = ExistsScopeError.PersistenceError(
                    context = context,
                    message = message,
                    cause = cause,
                    retryable = retryable,
                    errorCode = errorCode,
                    category = category
                )

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(null, normalizedTitle)
                } returns persistenceError.left()

                // When
                val result = service.validateTitleUniqueness(title, null)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<DomainInfrastructureError>()

                val infrastructureError = error as DomainInfrastructureError
                val databaseError = infrastructureError.repositoryError as RepositoryError.DatabaseError
                databaseError.message shouldContain "errorCode=none"  // Should handle null gracefully
                databaseError.message shouldContain "context=$context"
                databaseError.message shouldContain "retryable=$retryable"
            }

            it("should map PersistenceError with different ErrorCategory values") {
                // Test each ErrorCategory to ensure proper mapping
                val categories = ExistsScopeError.PersistenceError.ErrorCategory.values()

                categories.forEach { category ->
                    // Given
                    val title = "Test Title $category"
                    val normalizedTitle = title.lowercase().trim()
                    val context = ExistsScopeError.ExistenceContext.ByCustomCriteria(
                        mapOf("category" to category.name)
                    )
                    val persistenceError = ExistsScopeError.PersistenceError(
                        context = context,
                        message = "Error with category $category",
                        cause = RuntimeException("Category test"),
                        retryable = true,
                        errorCode = "CAT_${category.name}",
                        category = category
                    )

                    coEvery {
                        mockScopeRepository.existsByParentIdAndTitle(null, normalizedTitle)
                    } returns persistenceError.left()

                    // When
                    val result = service.validateTitleUniqueness(title, null)

                    // Then
                    result.isLeft() shouldBe true
                    val error = result.leftOrNull()!!
                    error should beInstanceOf<DomainInfrastructureError>()

                    val infrastructureError = error as DomainInfrastructureError
                    infrastructureError.repositoryError should beInstanceOf<RepositoryError.DatabaseError>()
                }
            }
        }

        describe("UnknownError error mapping") {
            it("should map UnknownError to DomainInfrastructureError with RepositoryError.UnknownError") {
                // Given
                val parentId = ScopeId.generate()
                val title = "Test Title"
                val normalizedTitle = title.lowercase().trim()
                val message = "Unexpected error occurred during existence check"
                val cause = IllegalStateException("Unknown state reached")

                val unknownError = ExistsScopeError.UnknownError(
                    message = message,
                    cause = cause
                )

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(parentId, normalizedTitle)
                } returns unknownError.left()

                // When
                val result = service.validateTitleUniqueness(title, parentId)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<DomainInfrastructureError>()

                val infrastructureError = error as DomainInfrastructureError
                infrastructureError.repositoryError should beInstanceOf<RepositoryError.UnknownError>()

                val repositoryUnknownError = infrastructureError.repositoryError as RepositoryError.UnknownError
                repositoryUnknownError.message shouldContain "Unknown error during existence check"
                repositoryUnknownError.message shouldContain message
                repositoryUnknownError.cause shouldBe cause
            }

            it("should handle UnknownError with null cause") {
                // Given
                val title = "Test Title"
                val normalizedTitle = title.lowercase().trim()
                val message = "Null cause unknown error"
                val cause = RuntimeException("Exception with null cause", null)

                val unknownError = ExistsScopeError.UnknownError(
                    message = message,
                    cause = cause
                )

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(null, normalizedTitle)
                } returns unknownError.left()

                // When
                val result = service.validateTitleUniqueness(title, null)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<DomainInfrastructureError>()

                val infrastructureError = error as DomainInfrastructureError
                val repositoryUnknownError = infrastructureError.repositoryError as RepositoryError.UnknownError

                // Verify no NullPointerException when accessing cause?.message
                val causeMessage = repositoryUnknownError.cause.cause?.message
                causeMessage shouldBe null  // Should be null, not throw NPE
            }
        }

        describe("successful existence check") {
            it("should return success when no duplicates exist") {
                // Given
                val parentId = ScopeId.generate()
                val title = "Unique Title"
                val normalizedTitle = title.lowercase().trim()

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(parentId, normalizedTitle)
                } returns false.right()

                // When
                val result = service.validateTitleUniqueness(title, parentId)

                // Then
                result.isRight() shouldBe true
            }

            it("should fail validation when duplicate exists (business rule violation)") {
                // Given
                val parentId = ScopeId.generate()
                val title = "Duplicate Title"
                val normalizedTitle = title.lowercase().trim()

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(parentId, normalizedTitle)
                } returns true.right()

                // When
                val result = service.validateTitleUniqueness(title, parentId)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<io.github.kamiazya.scopes.domain.error.ScopeBusinessRuleViolation.ScopeDuplicateTitle>()
            }
        }
    }
})
