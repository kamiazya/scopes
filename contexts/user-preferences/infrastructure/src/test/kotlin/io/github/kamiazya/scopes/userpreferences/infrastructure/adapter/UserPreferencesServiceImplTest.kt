package io.github.kamiazya.scopes.userpreferences.infrastructure.adapter

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.interfaces.shared.dto.HierarchyPreferencesDto
import io.github.kamiazya.scopes.interfaces.shared.dto.UserPreferencesDto
import io.github.kamiazya.scopes.interfaces.shared.error.UserPreferencesServiceError
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.userpreferences.domain.aggregate.UserPreferencesAggregate
import io.github.kamiazya.scopes.userpreferences.domain.entity.UserPreferences
import io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError
import io.github.kamiazya.scopes.userpreferences.domain.repository.UserPreferencesRepository
import io.github.kamiazya.scopes.userpreferences.domain.value.HierarchyPreferences
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class UserPreferencesServiceImplTest :
    DescribeSpec({

        beforeEach {
            clearAllMocks()
        }

        describe("UserPreferencesServiceImpl") {
            val mockRepository = mockk<UserPreferencesRepository>()
            val mockLogger = mockk<Logger>(relaxed = true)
            val mockClock = mockk<Clock>()
            val fixedInstant = Instant.fromEpochSeconds(1640995200)
            val service = UserPreferencesServiceImpl(mockRepository, mockLogger, mockClock)

            describe("when user preferences exist") {
                it("should return existing preferences successfully") {
                    // Given
                    val hierarchyPreferences = HierarchyPreferences.create(10, 20).getOrNull()!!
                    val userPreferences = UserPreferences(
                        hierarchyPreferences = hierarchyPreferences,
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant,
                    )
                    val aggregate = UserPreferencesAggregate(
                        id = AggregateId.Simple.generate(),
                        version = AggregateVersion.initial(),
                        preferences = userPreferences,
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant,
                    )

                    coEvery { mockRepository.findForCurrentUser() } returns aggregate.right()

                    // When
                    val result = runBlocking { service.getCurrentUserPreferences() }

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { dto ->
                            dto.hierarchyPreferences.maxDepth shouldBe 10
                            dto.hierarchyPreferences.maxChildrenPerScope shouldBe 20
                        },
                    )

                    verify { mockLogger.debug("Getting current user preferences") }
                    verify { mockLogger.debug("Successfully retrieved user preferences") }
                    coVerify(exactly = 1) { mockRepository.findForCurrentUser() }
                    coVerify(exactly = 0) { mockRepository.save(any()) }
                }

                it("should handle unlimited preferences (null values)") {
                    // Given
                    val hierarchyPreferences = HierarchyPreferences.create(null, null).getOrNull()!!
                    val userPreferences = UserPreferences(
                        hierarchyPreferences = hierarchyPreferences,
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant,
                    )
                    val aggregate = UserPreferencesAggregate(
                        id = AggregateId.Simple.generate(),
                        version = AggregateVersion.initial(),
                        preferences = userPreferences,
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant,
                    )

                    coEvery { mockRepository.findForCurrentUser() } returns aggregate.right()

                    // When
                    val result = runBlocking { service.getCurrentUserPreferences() }

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { dto ->
                            dto.hierarchyPreferences.maxDepth shouldBe null
                            dto.hierarchyPreferences.maxChildrenPerScope shouldBe null
                        },
                    )
                }

                it("should handle mixed null and valid preferences") {
                    // Given
                    val hierarchyPreferences = HierarchyPreferences.create(15, null).getOrNull()!!
                    val userPreferences = UserPreferences(
                        hierarchyPreferences = hierarchyPreferences,
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant,
                    )
                    val aggregate = UserPreferencesAggregate(
                        id = AggregateId.Simple.generate(),
                        version = AggregateVersion.initial(),
                        preferences = userPreferences,
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant,
                    )

                    coEvery { mockRepository.findForCurrentUser() } returns aggregate.right()

                    // When
                    val result = runBlocking { service.getCurrentUserPreferences() }

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { dto ->
                            dto.hierarchyPreferences.maxDepth shouldBe 15
                            dto.hierarchyPreferences.maxChildrenPerScope shouldBe null
                        },
                    )
                }
            }

            describe("when no user preferences exist") {
                it("should create and save default preferences successfully") {
                    // Given
                    coEvery { mockRepository.findForCurrentUser() } returns null.right()
                    coEvery { mockClock.now() } returns fixedInstant
                    coEvery { mockRepository.save(any()) } returns Unit.right()

                    // When
                    val result = runBlocking { service.getCurrentUserPreferences() }

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { dto ->
                            dto.hierarchyPreferences.maxDepth shouldBe null
                            dto.hierarchyPreferences.maxChildrenPerScope shouldBe null
                        },
                    )

                    verify { mockLogger.debug("Getting current user preferences") }
                    verify { mockLogger.debug("Successfully retrieved user preferences") }
                    coVerify(exactly = 1) { mockRepository.findForCurrentUser() }
                    coVerify(exactly = 1) { mockRepository.save(any()) }
                }

                it("should handle aggregate creation failure") {
                    // Given - Aggregate creation uses static method, so we test save failure instead
                    coEvery { mockRepository.findForCurrentUser() } returns null.right()
                    coEvery { mockClock.now() } returns fixedInstant

                    val saveError = UserPreferencesError.InvalidPreferenceValue("save", "test", "Disk full")
                    coEvery { mockRepository.save(any()) } returns saveError.left()

                    // When
                    val result = runBlocking { service.getCurrentUserPreferences() }

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error shouldBe UserPreferencesServiceError.PreferencesWriteError(
                                cause = saveError.message,
                                configPath = null,
                            )
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )

                    verify { mockLogger.error("Failed to save default preferences: ${saveError.message}") }
                }

                it("should handle newly created aggregate with no preferences") {
                    // Given - This is a defensive scenario that shouldn't happen in practice
                    coEvery { mockRepository.findForCurrentUser() } returns null.right()
                    coEvery { mockClock.now() } returns fixedInstant
                    coEvery { mockRepository.save(any()) } returns Unit.right()

                    // We need to mock the aggregate creation to return one without preferences
                    // Since this uses static methods, this test verifies the error handling path

                    // When - this scenario is hard to test directly due to static method
                    // We'll test the other path where existing aggregate has no preferences
                    val corruptedAggregate = UserPreferencesAggregate(
                        id = AggregateId.Simple.generate(),
                        version = AggregateVersion.initial(),
                        preferences = null, // No preferences
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant,
                    )

                    coEvery { mockRepository.findForCurrentUser() } returns corruptedAggregate.right()

                    val result = runBlocking { service.getCurrentUserPreferences() }

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error shouldBe UserPreferencesServiceError.PreferencesCorrupted(
                                details = "Aggregate exists but has no preferences",
                                configPath = null,
                            )
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )
                }
            }

            describe("when repository returns domain errors") {
                it("should map InvalidPreferenceValue to PreferencesCorrupted") {
                    // Given
                    val domainError = UserPreferencesError.InvalidPreferenceValue("config", "invalid", "Bad format")
                    coEvery { mockRepository.findForCurrentUser() } returns domainError.left()

                    // When
                    val result = runBlocking { service.getCurrentUserPreferences() }

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error shouldBe UserPreferencesServiceError.PreferencesCorrupted(
                                details = domainError.message,
                                configPath = null,
                            )
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )

                    verify { mockLogger.error("Failed to get user preferences: ${domainError.message}") }
                }

                it("should map other domain errors to PreferencesReadError") {
                    // Given
                    val domainError = UserPreferencesError.PreferencesNotInitialized()
                    coEvery { mockRepository.findForCurrentUser() } returns domainError.left()

                    // When
                    val result = runBlocking { service.getCurrentUserPreferences() }

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error shouldBe UserPreferencesServiceError.PreferencesReadError(
                                cause = domainError.message,
                                configPath = null,
                            )
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )

                    verify { mockLogger.error("Failed to get user preferences: ${domainError.message}") }
                }

                it("should map InvalidHierarchyPreferences to PreferencesReadError") {
                    // Given
                    val domainError = UserPreferencesError.InvalidHierarchyPreferences("Invalid depth")
                    coEvery { mockRepository.findForCurrentUser() } returns domainError.left()

                    // When
                    val result = runBlocking { service.getCurrentUserPreferences() }

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error shouldBe UserPreferencesServiceError.PreferencesReadError(
                                cause = domainError.message,
                                configPath = null,
                            )
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )
                }
            }

            describe("logging behavior") {
                it("should log debug messages for successful operations") {
                    // Given
                    val hierarchyPreferences = HierarchyPreferences.create(5, 10).getOrNull()!!
                    val userPreferences = UserPreferences(
                        hierarchyPreferences = hierarchyPreferences,
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant,
                    )
                    val aggregate = UserPreferencesAggregate(
                        id = AggregateId.Simple.generate(),
                        version = AggregateVersion.initial(),
                        preferences = userPreferences,
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant,
                    )

                    coEvery { mockRepository.findForCurrentUser() } returns aggregate.right()

                    // When
                    runBlocking { service.getCurrentUserPreferences() }

                    // Then
                    verify { mockLogger.debug("Getting current user preferences") }
                    verify { mockLogger.debug("Successfully retrieved user preferences") }
                }

                it("should log error messages for failed operations") {
                    // Given
                    val domainError = UserPreferencesError.InvalidPreferenceValue("test", "value", "Test error")
                    coEvery { mockRepository.findForCurrentUser() } returns domainError.left()

                    // When
                    runBlocking { service.getCurrentUserPreferences() }

                    // Then
                    verify { mockLogger.debug("Getting current user preferences") }
                    verify { mockLogger.error("Failed to get user preferences: ${domainError.message}") }
                    verify(exactly = 0) { mockLogger.debug("Successfully retrieved user preferences") }
                }

                it("should log specific error messages for save failures") {
                    // Given
                    coEvery { mockRepository.findForCurrentUser() } returns null.right()
                    coEvery { mockClock.now() } returns fixedInstant

                    val saveError = UserPreferencesError.InvalidPreferenceValue("save", "test", "Save failed")
                    coEvery { mockRepository.save(any()) } returns saveError.left()

                    // When
                    runBlocking { service.getCurrentUserPreferences() }

                    // Then
                    verify { mockLogger.debug("Getting current user preferences") }
                    verify { mockLogger.error("Failed to save default preferences: ${saveError.message}") }
                    verify(exactly = 0) { mockLogger.debug("Successfully retrieved user preferences") }
                }
            }

            describe("integration behavior") {
                it("should handle multiple consecutive calls correctly") {
                    // Given
                    val hierarchyPreferences = HierarchyPreferences.create(3, 6).getOrNull()!!
                    val userPreferences = UserPreferences(
                        hierarchyPreferences = hierarchyPreferences,
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant,
                    )
                    val aggregate = UserPreferencesAggregate(
                        id = AggregateId.Simple.generate(),
                        version = AggregateVersion.initial(),
                        preferences = userPreferences,
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant,
                    )

                    coEvery { mockRepository.findForCurrentUser() } returns aggregate.right()

                    // When - make multiple calls
                    val results = runBlocking {
                        (1..3).map { service.getCurrentUserPreferences() }
                    }

                    // Then - all should succeed with same preferences
                    results.forEach { result ->
                        result.isRight() shouldBe true
                        result.fold(
                            { throw AssertionError("Expected Right but got Left: $it") },
                            { dto ->
                                dto.hierarchyPreferences.maxDepth shouldBe 3
                                dto.hierarchyPreferences.maxChildrenPerScope shouldBe 6
                            },
                        )
                    }

                    // Verify repository was called for each request
                    coVerify(exactly = 3) { mockRepository.findForCurrentUser() }

                    // Verify logging happened for each call
                    verify(exactly = 3) { mockLogger.debug("Getting current user preferences") }
                    verify(exactly = 3) { mockLogger.debug("Successfully retrieved user preferences") }
                }

                it("should properly map domain preferences to service DTOs") {
                    // Given - preferences with specific values
                    val hierarchyPreferences = HierarchyPreferences.create(25, 50).getOrNull()!!
                    val userPreferences = UserPreferences(
                        hierarchyPreferences = hierarchyPreferences,
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant,
                    )
                    val aggregate = UserPreferencesAggregate(
                        id = AggregateId.Simple.generate(),
                        version = AggregateVersion.initial(),
                        preferences = userPreferences,
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant,
                    )

                    coEvery { mockRepository.findForCurrentUser() } returns aggregate.right()

                    // When
                    val result = runBlocking { service.getCurrentUserPreferences() }

                    // Then - verify exact mapping
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { dto ->
                            // Verify the DTO structure matches expected
                            dto shouldBe UserPreferencesDto(
                                hierarchyPreferences = HierarchyPreferencesDto(
                                    maxDepth = 25,
                                    maxChildrenPerScope = 50,
                                ),
                            )
                        },
                    )
                }
            }
        }
    })
