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
 * Test to verify the GetCurrentUserPreferencesHandler properly behaves as a query handler
 * that only reads data and does not modify state.
 */
class GetCurrentUserPreferencesHandlerSuspendFixTest :
    DescribeSpec({
        describe("GetCurrentUserPreferencesHandler") {
            describe("query handler behavior") {
                it("should return PreferencesNotInitialized when no aggregate exists") {
                    // Given
                    val repository = mockk<UserPreferencesRepository>()
                    val handler = GetCurrentUserPreferencesHandler(repository)

                    coEvery { repository.findForCurrentUser() } returns null.right()

                    // When
                    val result = handler(GetCurrentUserPreferences)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull() shouldBe UserPreferencesError.PreferencesNotInitialized

                    // Query handlers should not modify state
                    coVerify(exactly = 1) { repository.findForCurrentUser() }
                    coVerify(exactly = 0) { repository.save(any()) }
                }

                it("should propagate repository read errors") {
                    // Given
                    val repository = mockk<UserPreferencesRepository>()
                    val handler = GetCurrentUserPreferencesHandler(repository)

                    val customError = UserPreferencesError.InvalidPreferenceValue("test", "value", UserPreferencesError.ValidationError.INVALID_FORMAT)

                    coEvery { repository.findForCurrentUser() } returns customError.left()

                    // When
                    val result = handler(GetCurrentUserPreferences)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull() shouldBe customError

                    coVerify(exactly = 1) { repository.findForCurrentUser() }
                    coVerify(exactly = 0) { repository.save(any()) } // Query handler should not save
                }

                it("should successfully return existing preferences") {
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

                    coVerify(exactly = 1) { repository.findForCurrentUser() }
                    coVerify(exactly = 0) { repository.save(any()) } // Query handler should not save
                }
            }
        }
    })
