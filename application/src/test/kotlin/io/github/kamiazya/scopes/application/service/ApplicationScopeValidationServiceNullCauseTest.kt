package io.github.kamiazya.scopes.application.service

import arrow.core.left
import io.github.kamiazya.scopes.domain.error.ExistsScopeError
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.mockk.coEvery
import io.mockk.mockk

/**
 * Test suite specifically for verifying null cause handling in domain errors.
 *
 * This test ensures that when domain errors have null causes, accessing
 * cause?.message or other cause properties doesn't result in NullPointerException.
 *
 * Covers all ExistsScopeError variants with explicit null cause scenarios.
 */
class ApplicationScopeValidationServiceNullCauseTest : DescribeSpec({

    val mockScopeRepository = mockk<ScopeRepository>()
    val service = ApplicationScopeValidationService(mockScopeRepository)

    describe("Domain errors with null cause handling") {

        describe("ConnectionFailure with null cause") {
            it("should handle ConnectionFailure with explicitly null cause") {
                // Given
                val title = "Test Title"
                val normalizedTitle = title.lowercase().trim()
                val connectionMessage = "Connection failed"
                val exceptionWithNullCause = RuntimeException("Connection error", null)

                val connectionFailureError = ExistsScopeError.ConnectionFailure(
                    message = connectionMessage,
                    cause = exceptionWithNullCause
                )

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(null, normalizedTitle)
                } returns connectionFailureError.left()

                // When
                val result = service.validateTitleUniqueness(title, null)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!

                // Should be able to safely access error properties without NPE
                val infrastructureError = error as io.github.kamiazya.scopes.domain.error.DomainInfrastructureError
                val connectionError = infrastructureError.repositoryError as io.github.kamiazya.scopes.domain.error.RepositoryError.ConnectionError

                // Verify cause information is properly captured without Throwable reference issues
                connectionError.causeClass shouldBe RuntimeException::class
                connectionError.causeMessage shouldBe "Connection error"
            }

            it("should handle ConnectionFailure with nested null causes") {
                // Given
                val parentId = ScopeId.generate()
                val title = "Test Title"
                val normalizedTitle = title.lowercase().trim()

                // Create a chain with multiple null causes
                val deepestException = RuntimeException("Root cause", null)
                val middleException = RuntimeException("Middle cause", deepestException)
                val topException = RuntimeException("Top cause", middleException)

                val connectionFailureError = ExistsScopeError.ConnectionFailure(
                    message = "Multi-level connection failure",
                    cause = topException
                )

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(parentId, normalizedTitle)
                } returns connectionFailureError.left()

                // When
                val result = service.validateTitleUniqueness(title, parentId)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!

                val infrastructureError = error as io.github.kamiazya.scopes.domain.error.DomainInfrastructureError
                val connectionError = infrastructureError.repositoryError as io.github.kamiazya.scopes.domain.error.RepositoryError.ConnectionError

                // Verify cause information is properly captured (top-level exception only)
                connectionError.causeClass shouldBe RuntimeException::class
                connectionError.causeMessage shouldBe "Top cause"
            }
        }

        describe("UnknownError with null cause") {
            it("should handle UnknownError with null cause") {
                // Given
                val title = "Test Title"
                val normalizedTitle = title.lowercase().trim()
                val unknownMessage = "Unknown error occurred"
                val exceptionWithNullCause = IllegalStateException("Unknown state", null)

                val unknownError = ExistsScopeError.UnknownError(
                    message = unknownMessage,
                    cause = exceptionWithNullCause
                )

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(null, normalizedTitle)
                } returns unknownError.left()

                // When
                val result = service.validateTitleUniqueness(title, null)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!

                val infrastructureError = error as io.github.kamiazya.scopes.domain.error.DomainInfrastructureError
                val repositoryUnknownError = infrastructureError.repositoryError as io.github.kamiazya.scopes.domain.error.RepositoryError.UnknownError

                // Verify cause information is properly captured
                repositoryUnknownError.causeClass shouldBe IllegalStateException::class
                repositoryUnknownError.causeMessage shouldBe "Unknown state"
            }
        }

        describe("PersistenceError with null cause") {
            it("should handle PersistenceError with null cause") {
                // Given
                val parentId = ScopeId.generate()
                val title = "Test Title"
                val normalizedTitle = title.lowercase().trim()
                val context = ExistsScopeError.ExistenceContext.ByParentIdAndTitle(parentId, title)
                val message = "Persistence operation failed"
                val exceptionWithNullCause = java.sql.SQLException("SQL error").apply { initCause(null) }

                val persistenceError = ExistsScopeError.PersistenceError(
                    context = context,
                    message = message,
                    cause = exceptionWithNullCause,
                    retryable = false,
                    errorCode = "PERSISTENCE_NULL_CAUSE",
                    category = ExistsScopeError.PersistenceError.ErrorCategory.DATABASE
                )

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(parentId, normalizedTitle)
                } returns persistenceError.left()

                // When
                val result = service.validateTitleUniqueness(title, parentId)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!

                val infrastructureError = error as io.github.kamiazya.scopes.domain.error.DomainInfrastructureError
                val databaseError = infrastructureError.repositoryError as io.github.kamiazya.scopes.domain.error.RepositoryError.DatabaseError

                // Verify cause information is properly captured
                databaseError.causeClass shouldBe java.sql.SQLException::class
                databaseError.causeMessage shouldBe "SQL error"
            }
        }

        describe("Complex null cause scenarios") {
            it("should handle recursive cause access without NPE") {
                // Given
                val title = "Deep Cause Test"
                val normalizedTitle = title.lowercase().trim()

                // Create an exception with null cause
                val nullCauseException = RuntimeException("Exception with null cause", null)

                val connectionFailure = ExistsScopeError.ConnectionFailure(
                    message = "Deep cause test",
                    cause = nullCauseException
                )

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(null, normalizedTitle)
                } returns connectionFailure.left()

                // When
                val result = service.validateTitleUniqueness(title, null)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!

                val infrastructureError = error as io.github.kamiazya.scopes.domain.error.DomainInfrastructureError
                val connectionError = infrastructureError.repositoryError as io.github.kamiazya.scopes.domain.error.RepositoryError.ConnectionError

                // With new approach, we safely store cause info without Throwable chains
                connectionError.causeClass shouldBe RuntimeException::class
                connectionError.causeMessage shouldBe "Exception with null cause"
            }

            it("should handle toString() calls on null causes") {
                // Given
                val title = "ToString Test"
                val normalizedTitle = title.lowercase().trim()
                val nullCauseException = RuntimeException("ToString test", null)

                val unknownError = ExistsScopeError.UnknownError(
                    message = "ToString null cause test",
                    cause = nullCauseException
                )

                coEvery {
                    mockScopeRepository.existsByParentIdAndTitle(null, normalizedTitle)
                } returns unknownError.left()

                // When
                val result = service.validateTitleUniqueness(title, null)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!

                val infrastructureError = error as io.github.kamiazya.scopes.domain.error.DomainInfrastructureError
                val repositoryUnknownError = infrastructureError.repositoryError as io.github.kamiazya.scopes.domain.error.RepositoryError.UnknownError

                // Verify safe cause information storage without toString() issues
                repositoryUnknownError.causeClass shouldBe RuntimeException::class
                repositoryUnknownError.causeMessage shouldBe "ToString test"
            }
        }
    }
})
