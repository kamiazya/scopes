package io.github.kamiazya.scopes.interfaces.shared.adapters

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.interfaces.shared.dto.HierarchyPreferencesDto
import io.github.kamiazya.scopes.interfaces.shared.dto.UserPreferencesDto
import io.github.kamiazya.scopes.interfaces.shared.error.UserPreferencesServiceError
import io.github.kamiazya.scopes.interfaces.shared.services.UserPreferencesService
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.HierarchyPolicy
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

class UserPreferencesToHierarchyPolicyAdapterTest :
    DescribeSpec({

        beforeEach {
            clearAllMocks()
        }

        describe("UserPreferencesToHierarchyPolicyAdapter") {
            val mockUserPreferencesService = mockk<UserPreferencesService>()
            val mockLogger = mockk<Logger>(relaxed = true)
            val adapter = UserPreferencesToHierarchyPolicyAdapter(mockUserPreferencesService, mockLogger)

            describe("when user preferences service returns successful preferences") {
                it("should translate user preferences with both values to hierarchy policy correctly") {
                    // Given
                    val hierarchyPreferences = HierarchyPreferencesDto(10, 20)
                    val userPreferences = UserPreferencesDto(hierarchyPreferences)
                    coEvery { mockUserPreferencesService.getCurrentUserPreferences() } returns userPreferences.right()

                    // When
                    val result = runBlocking { adapter.getPolicy() }

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { policy ->
                            policy.maxDepth shouldBe 10
                            policy.maxChildrenPerScope shouldBe 20
                            policy.isDepthUnlimited() shouldBe false
                            policy.isChildrenPerScopeUnlimited() shouldBe false
                        },
                    )
                }

                it("should handle null preferences correctly (unlimited policy)") {
                    // Given
                    val hierarchyPreferences = HierarchyPreferencesDto(null, null)
                    val userPreferences = UserPreferencesDto(hierarchyPreferences)
                    coEvery { mockUserPreferencesService.getCurrentUserPreferences() } returns userPreferences.right()

                    // When
                    val result = runBlocking { adapter.getPolicy() }

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { policy ->
                            policy shouldBe HierarchyPolicy.default()
                            policy.isDepthUnlimited() shouldBe true
                            policy.isChildrenPerScopeUnlimited() shouldBe true
                        },
                    )
                }

                it("should handle mixed null and valid preferences correctly") {
                    // Given - only maxDepth set
                    val hierarchyPreferences1 = HierarchyPreferencesDto(10, null)
                    val userPreferences1 = UserPreferencesDto(hierarchyPreferences1)
                    coEvery { mockUserPreferencesService.getCurrentUserPreferences() } returns userPreferences1.right()

                    // When
                    val result1 = runBlocking { adapter.getPolicy() }

                    // Then
                    result1.isRight() shouldBe true
                    result1.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { policy ->
                            policy.maxDepth shouldBe 10
                            policy.maxChildrenPerScope shouldBe null
                            policy.isDepthUnlimited() shouldBe false
                            policy.isChildrenPerScopeUnlimited() shouldBe true
                        },
                    )
                }

                it("should handle only maxChildren set correctly") {
                    // Given - only maxChildren set
                    val hierarchyPreferences = HierarchyPreferencesDto(null, 20)
                    val userPreferences = UserPreferencesDto(hierarchyPreferences)
                    coEvery { mockUserPreferencesService.getCurrentUserPreferences() } returns userPreferences.right()

                    // When
                    val result = runBlocking { adapter.getPolicy() }

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { policy ->
                            policy.maxDepth shouldBe null
                            policy.maxChildrenPerScope shouldBe 20
                            policy.isDepthUnlimited() shouldBe true
                            policy.isChildrenPerScopeUnlimited() shouldBe false
                        },
                    )
                }
            }

            describe("when user preferences service returns errors") {
                it("should fallback to default policy on read error and log warning") {
                    // Given
                    val serviceError = UserPreferencesServiceError.PreferencesReadError("File not found", "/path/to/config")
                    coEvery { mockUserPreferencesService.getCurrentUserPreferences() } returns serviceError.left()

                    // When
                    val result = runBlocking { adapter.getPolicy() }

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { policy ->
                            policy shouldBe HierarchyPolicy.default()
                            policy.isDepthUnlimited() shouldBe true
                            policy.isChildrenPerScopeUnlimited() shouldBe true
                        },
                    )

                    // Verify service was called
                    coVerify { mockUserPreferencesService.getCurrentUserPreferences() }
                }

                it("should fallback to default policy on corrupted preferences error") {
                    // Given
                    val error = UserPreferencesServiceError.PreferencesCorrupted("Invalid JSON", "/config/prefs.json")
                    coEvery { mockUserPreferencesService.getCurrentUserPreferences() } returns error.left()

                    // When
                    val result = runBlocking { adapter.getPolicy() }

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { policy -> policy shouldBe HierarchyPolicy.default() },
                    )
                }

                it("should fallback to default policy on migration required error") {
                    // Given
                    val error = UserPreferencesServiceError.PreferencesMigrationRequired("1.0", "2.0")
                    coEvery { mockUserPreferencesService.getCurrentUserPreferences() } returns error.left()

                    // When
                    val result = runBlocking { adapter.getPolicy() }

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { policy -> policy shouldBe HierarchyPolicy.default() },
                    )
                }
            }

            describe("when user preferences contain invalid values") {
                it("should fallback to default policy for invalid maxDepth") {
                    // Given
                    val hierarchyPreferences = HierarchyPreferencesDto(-1, 10) // Invalid negative depth
                    val userPreferences = UserPreferencesDto(hierarchyPreferences)
                    coEvery { mockUserPreferencesService.getCurrentUserPreferences() } returns userPreferences.right()

                    // When
                    val result = runBlocking { adapter.getPolicy() }

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { policy -> policy shouldBe HierarchyPolicy.default() },
                    )
                }

                it("should fallback to default policy for invalid maxChildrenPerScope") {
                    // Given
                    val hierarchyPreferences = HierarchyPreferencesDto(5, 0) // Invalid zero children
                    val userPreferences = UserPreferencesDto(hierarchyPreferences)
                    coEvery { mockUserPreferencesService.getCurrentUserPreferences() } returns userPreferences.right()

                    // When
                    val result = runBlocking { adapter.getPolicy() }

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { policy -> policy shouldBe HierarchyPolicy.default() },
                    )
                }

                it("should fallback to default policy for both invalid values") {
                    // Given
                    val hierarchyPreferences = HierarchyPreferencesDto(-5, -10) // Both invalid
                    val userPreferences = UserPreferencesDto(hierarchyPreferences)
                    coEvery { mockUserPreferencesService.getCurrentUserPreferences() } returns userPreferences.right()

                    // When
                    val result = runBlocking { adapter.getPolicy() }

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { policy -> policy shouldBe HierarchyPolicy.default() },
                    )
                }
            }

            describe("integration behavior") {
                it("should call user preferences service exactly once per getPolicy call") {
                    // Given
                    val hierarchyPreferences = HierarchyPreferencesDto(10, 20)
                    val userPreferences = UserPreferencesDto(hierarchyPreferences)
                    coEvery { mockUserPreferencesService.getCurrentUserPreferences() } returns userPreferences.right()

                    // When
                    runBlocking {
                        adapter.getPolicy()
                        adapter.getPolicy()
                    }

                    // Then
                    coVerify(exactly = 2) { mockUserPreferencesService.getCurrentUserPreferences() }
                }

                it("should handle multiple calls correctly") {
                    // Given
                    val hierarchyPreferences = HierarchyPreferencesDto(15, 25)
                    val userPreferences = UserPreferencesDto(hierarchyPreferences)
                    coEvery { mockUserPreferencesService.getCurrentUserPreferences() } returns userPreferences.right()

                    // When - make multiple calls
                    val results = runBlocking {
                        (1..3).map { adapter.getPolicy() }
                    }

                    // Then - all should succeed with same policy
                    results.forEach { result ->
                        result.isRight() shouldBe true
                        result.fold(
                            { throw AssertionError("Expected Right but got Left: $it") },
                            { policy ->
                                policy.maxDepth shouldBe 15
                                policy.maxChildrenPerScope shouldBe 25
                            },
                        )
                    }

                    // Verify service was called for each request
                    coVerify(exactly = 3) { mockUserPreferencesService.getCurrentUserPreferences() }
                }
            }
        }
    })
