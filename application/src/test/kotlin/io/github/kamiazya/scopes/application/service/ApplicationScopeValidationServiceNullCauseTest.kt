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

                // Verify safe access to cause chain without NPE
                val topLevelCause = connectionError.cause
                val nestedCause = topLevelCause.cause
                val nestedCauseMessage = topLevelCause.cause?.message

                topLevelCause should beInstanceOf<RuntimeException>()
                nestedCause.should(beNull())
                nestedCauseMessage.should(beNull())
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

                // Verify safe traversal of cause chain
                val level1Cause = connectionError.cause
                val level2Cause = level1Cause.cause
                val level3Cause = level2Cause?.cause
                val level4Cause = level3Cause?.cause  // This should be null

                level1Cause.message shouldBe "Top cause"
                level2Cause?.message shouldBe "Middle cause"
                level3Cause?.message shouldBe "Root cause"
                level4Cause.should(beNull())

                // Verify no NPE when accessing properties on null cause
                val nullCauseMessage = level4Cause?.message
                nullCauseMessage.should(beNull())
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

                // Verify safe access to cause properties
                val causeMessage = repositoryUnknownError.cause.message
                val nestedCause = repositoryUnknownError.cause.cause
                val nestedCauseMessage = repositoryUnknownError.cause.cause?.message

                causeMessage shouldBe "Unknown state"
                nestedCause.should(beNull())
                nestedCauseMessage.should(beNull())
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

                // Verify safe access to SQLException properties with null cause
                val sqlCause = databaseError.cause as java.sql.SQLException
                val sqlNestedCause = sqlCause.cause
                val sqlCauseMessage = sqlCause.cause?.message

                sqlCause.message shouldBe "SQL error"
                sqlNestedCause.should(beNull())
                sqlCauseMessage.should(beNull())
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

                // Perform deep cause chain traversal that should be safe
                val cause1 = connectionError.cause
                val cause2 = cause1.cause
                val cause3 = cause2?.cause
                val cause4 = cause3?.cause
                val cause5 = cause4?.cause

                // All these accesses should be safe and not throw NPE
                cause1.message shouldBe "Exception with null cause"
                cause2.should(beNull())
                cause3.should(beNull())
                cause4.should(beNull())
                cause5.should(beNull())

                // Verify safe property access on null causes
                val message2 = cause2?.message
                val message3 = cause3?.message
                val stackTrace2 = cause2?.stackTrace
                val localizedMessage2 = cause2?.localizedMessage

                message2.should(beNull())
                message3.should(beNull())
                stackTrace2.should(beNull())
                localizedMessage2.should(beNull())
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

                // These operations should not throw NPE
                val causeString = repositoryUnknownError.cause.toString()
                val nestedCauseString = repositoryUnknownError.cause.cause?.toString()

                causeString shouldBe "java.lang.RuntimeException: ToString test"
                nestedCauseString.should(beNull())
            }
        }
    }
})
