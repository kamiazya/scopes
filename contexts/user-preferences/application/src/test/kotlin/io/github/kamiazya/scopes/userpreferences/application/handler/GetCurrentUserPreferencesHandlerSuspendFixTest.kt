package io.github.kamiazya.scopes.userpreferences.application.handler

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.userpreferences.application.query.GetCurrentUserPreferences
import io.github.kamiazya.scopes.userpreferences.domain.aggregate.UserPreferencesAggregate
import io.github.kamiazya.scopes.userpreferences.domain.entity.UserPreferences
import io.github.kamiazya.scopes.userpreferences.domain.value.HierarchyPreferences
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError
import io.github.kamiazya.scopes.userpreferences.domain.repository.UserPreferencesRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.datetime.Clock

/**
 * Test to verify the refactored createDefaultPreferences method properly handles
 * suspend functions within the either { } context.
 */
class GetCurrentUserPreferencesHandlerSuspendFixTest : DescribeSpec({
    describe("GetCurrentUserPreferencesHandler") {
        describe("createDefaultPreferences") {
            it("should handle PreferencesAlreadyInitialized error and reload from repository") {
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
                
                // First call returns null (no preferences exist)
                coEvery { repository.findForCurrentUser() } returnsMany listOf(
                    null.right(),
                    existingAggregate.right(), // Second call returns existing aggregate
                )
                
                // save() returns PreferencesAlreadyInitialized error (race condition)
                coEvery { repository.save(any()) } returns UserPreferencesError.PreferencesAlreadyInitialized().left()
                
                // When
                val result = handler(GetCurrentUserPreferences)
                
                // Then
                result.isRight() shouldBe true
                
                // Verify the sequence of calls
                coVerify(exactly = 2) { repository.findForCurrentUser() }
                coVerify(exactly = 1) { repository.save(any()) }
            }
            
            it("should propagate other errors from repository.save") {
                // Given
                val repository = mockk<UserPreferencesRepository>()
                val handler = GetCurrentUserPreferencesHandler(repository)
                
                val customError = UserPreferencesError.InvalidPreferenceValue("test", "value", "reason")
                
                coEvery { repository.findForCurrentUser() } returns null.right()
                coEvery { repository.save(any()) } returns customError.left()
                
                // When
                val result = handler(GetCurrentUserPreferences)
                
                // Then
                result.isLeft() shouldBe true
                result.leftOrNull() shouldBe customError
                
                coVerify(exactly = 1) { repository.findForCurrentUser() }
                coVerify(exactly = 1) { repository.save(any()) }
            }
            
            it("should successfully create and save default preferences") {
                // Given
                val repository = mockk<UserPreferencesRepository>()
                val handler = GetCurrentUserPreferencesHandler(repository)
                
                coEvery { repository.findForCurrentUser() } returns null.right()
                coEvery { repository.save(any()) } returns Unit.right()
                
                // When
                val result = handler(GetCurrentUserPreferences)
                
                // Then
                result.isRight() shouldBe true
                
                coVerify(exactly = 1) { repository.findForCurrentUser() }
                coVerify(exactly = 1) { repository.save(any()) }
            }
        }
    }
})