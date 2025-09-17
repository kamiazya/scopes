package io.github.kamiazya.scopes.userpreferences.application.handler.query

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.userpreferences.application.handler.query.GetCurrentUserPreferencesHandler
import io.github.kamiazya.scopes.userpreferences.application.query.GetCurrentUserPreferences
import io.github.kamiazya.scopes.userpreferences.domain.aggregate.UserPreferencesAggregate
import io.github.kamiazya.scopes.userpreferences.domain.entity.UserPreferences
import io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError
import io.github.kamiazya.scopes.userpreferences.domain.repository.UserPreferencesRepository
import io.github.kamiazya.scopes.userpreferences.domain.value.HierarchyPreferences
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.datetime.Clock

/**
 * Test to verify the query handler correctly handles read-only operations
 * and properly follows CQRS separation principles.
 */
class GetCurrentUserPreferencesHandlerSuspendFixTest :
    DescribeSpec({
        describe("GetCurrentUserPreferencesHandler") {
            describe("query behavior") {
                it("should return PreferencesNotInitialized when no preferences exist") {
                    // Given
                    val repository = mockk<UserPreferencesRepository>()
                    val handler = GetCurrentUserPreferencesHandler(repository)

                    // Repository returns null (no preferences exist)
                    coEvery { repository.findForCurrentUser() } returns null.right()

                    // When
                    val result = handler(GetCurrentUserPreferences)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull() shouldBe UserPreferencesError.PreferencesNotInitialized

                    // Verify only read operation was performed
                    coVerify(exactly = 1) { repository.findForCurrentUser() }
                    coVerify(exactly = 0) { repository.save(any()) }
                }

                it("should return PreferencesNotInitialized when aggregate exists but preferences are null") {
                    // Given
                    val repository = mockk<UserPreferencesRepository>()
                    val handler = GetCurrentUserPreferencesHandler(repository)

                    val now = Clock.System.now()
                    val aggregateWithoutPreferences = UserPreferencesAggregate(
                        id = AggregateId.Simple.generate(),
                        preferences = null, // No preferences initialized
                        version = AggregateVersion.initial(),
                        createdAt = now,
                        updatedAt = now,
                    )

                    coEvery { repository.findForCurrentUser() } returns aggregateWithoutPreferences.right()

                    // When
                    val result = handler(GetCurrentUserPreferences)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull() shouldBe UserPreferencesError.PreferencesNotInitialized

                    coVerify(exactly = 1) { repository.findForCurrentUser() }
                    coVerify(exactly = 0) { repository.save(any()) }
                }

                it("should successfully return preferences when they exist") {
                    // Given
                    val repository = mockk<UserPreferencesRepository>()
                    val handler = GetCurrentUserPreferencesHandler(repository)

                    val now = Clock.System.now()
                    val existingAggregate = UserPreferencesAggregate(
                        id = AggregateId.Simple.generate(),
                        preferences = UserPreferences(
                            hierarchyPreferences = HierarchyPreferences.DEFAULT,
                            createdAt = now,
                            updatedAt = now,
                        ),
                        version = AggregateVersion.initial(),
                        createdAt = now,
                        updatedAt = now,
                    )

                    coEvery { repository.findForCurrentUser() } returns existingAggregate.right()

                    // When
                    val result = handler(GetCurrentUserPreferences)

                    // Then
                    result.shouldBeRight()

                    // Verify only read operation was performed
                    coVerify(exactly = 1) { repository.findForCurrentUser() }
                    coVerify(exactly = 0) { repository.save(any()) }
                }
            }
        }
    })
