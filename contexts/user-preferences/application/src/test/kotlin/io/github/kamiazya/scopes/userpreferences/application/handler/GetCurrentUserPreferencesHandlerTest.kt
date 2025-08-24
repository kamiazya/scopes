package io.github.kamiazya.scopes.userpreferences.application.handler

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.userpreferences.application.dto.UserPreferencesInternalDto
import io.github.kamiazya.scopes.userpreferences.application.query.GetCurrentUserPreferences
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
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class GetCurrentUserPreferencesHandlerTest :
    DescribeSpec({

        beforeEach {
            clearAllMocks()
        }

        describe("GetCurrentUserPreferencesHandler") {
            val mockRepository = mockk<UserPreferencesRepository>()
            val mockClock = mockk<Clock>()
            val fixedInstant = Instant.fromEpochSeconds(1640995200) // 2022-01-01 00:00:00
            val handler = GetCurrentUserPreferencesHandler(mockRepository, mockClock)

            describe("when preferences exist in repository") {
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
                    val result = handler.invoke(GetCurrentUserPreferences)

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { dto ->
                            dto.hierarchyPreferences.maxDepth shouldBe 10
                            dto.hierarchyPreferences.maxChildrenPerScope shouldBe 20
                            dto.createdAt shouldBe fixedInstant
                            dto.updatedAt shouldBe fixedInstant
                        },
                    )

                    coVerify(exactly = 1) { mockRepository.findForCurrentUser() }
                    coVerify(exactly = 0) { mockRepository.save(any()) }
                }

                it("should handle preferences with null values (unlimited settings)") {
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
                    val result = handler.invoke(GetCurrentUserPreferences)

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
            }

            describe("when no preferences exist in repository") {
                it("should create and save default preferences successfully") {
                    // Given
                    coEvery { mockRepository.findForCurrentUser() } returns null.right()
                    every { mockClock.now() } returns fixedInstant

                    val newAggregate = UserPreferencesAggregate(
                        id = AggregateId.Simple.generate(),
                        version = AggregateVersion.initial(),
                        preferences = UserPreferences(
                            hierarchyPreferences = HierarchyPreferences.DEFAULT,
                            createdAt = fixedInstant,
                            updatedAt = fixedInstant,
                        ),
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant,
                    )
                    coEvery { mockRepository.save(any()) } returns Unit.right()

                    // When
                    val result = handler.invoke(GetCurrentUserPreferences)

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { dto ->
                            dto.hierarchyPreferences.maxDepth shouldBe null
                            dto.hierarchyPreferences.maxChildrenPerScope shouldBe null
                        },
                    )

                    coVerify(exactly = 1) { mockRepository.findForCurrentUser() }
                    coVerify(exactly = 1) { mockRepository.save(any()) }
                }

                it("should handle save failure when creating default preferences") {
                    // Given
                    val saveError = UserPreferencesError.InvalidPreferenceValue("save", "test", "Database error")
                    coEvery { mockRepository.findForCurrentUser() } returns null.right()
                    every { mockClock.now() } returns fixedInstant
                    coEvery { mockRepository.save(any()) } returns saveError.left()

                    // When
                    val result = handler.invoke(GetCurrentUserPreferences)

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error shouldBe saveError
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )

                    coVerify(exactly = 1) { mockRepository.findForCurrentUser() }
                    coVerify(exactly = 1) { mockRepository.save(any()) }
                }
            }

            describe("when repository returns errors") {
                it("should propagate repository read errors") {
                    // Given
                    val repositoryError = UserPreferencesError.InvalidPreferenceValue("read", "config", "File corrupted")
                    coEvery { mockRepository.findForCurrentUser() } returns repositoryError.left()

                    // When
                    val result = handler.invoke(GetCurrentUserPreferences)

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error shouldBe repositoryError
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )

                    coVerify(exactly = 1) { mockRepository.findForCurrentUser() }
                    coVerify(exactly = 0) { mockRepository.save(any()) }
                }
            }

            describe("when aggregate exists but preferences are not initialized") {
                it("should return PreferencesNotInitialized error") {
                    // Given
                    val aggregate = UserPreferencesAggregate(
                        id = AggregateId.Simple.generate(),
                        version = AggregateVersion.initial(),
                        preferences = null, // Preferences not initialized
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant,
                    )

                    coEvery { mockRepository.findForCurrentUser() } returns aggregate.right()

                    // When
                    val result = handler.invoke(GetCurrentUserPreferences)

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error shouldBe UserPreferencesError.PreferencesNotInitialized()
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )
                }
            }

            describe("edge cases and concurrent scenarios") {
                it("should handle multiple consecutive calls correctly") {
                    // Given
                    val hierarchyPreferences = HierarchyPreferences.create(5, 15).getOrNull()!!
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
                    val results = (1..3).map { handler.invoke(GetCurrentUserPreferences) }

                    // Then - all should succeed with same preferences
                    results.forEach { result ->
                        result.isRight() shouldBe true
                        result.fold(
                            { throw AssertionError("Expected Right but got Left: $it") },
                            { dto ->
                                dto.hierarchyPreferences.maxDepth shouldBe 5
                                dto.hierarchyPreferences.maxChildrenPerScope shouldBe 15
                            },
                        )
                    }

                    coVerify(exactly = 3) { mockRepository.findForCurrentUser() }
                }

                it("should handle query object correctly") {
                    // Given
                    val hierarchyPreferences = HierarchyPreferences.create(3, 7).getOrNull()!!
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
                    val result = handler.invoke(GetCurrentUserPreferences)

                    // Then
                    result.isRight() shouldBe true
                    // Query object should not affect the result - it's a data object
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { dto ->
                            dto shouldBe UserPreferencesInternalDto.from(userPreferences)
                        },
                    )
                }
            }
        }
    })
