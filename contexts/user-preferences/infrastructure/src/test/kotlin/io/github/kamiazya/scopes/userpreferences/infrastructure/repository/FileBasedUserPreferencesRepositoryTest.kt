package io.github.kamiazya.scopes.userpreferences.infrastructure.repository

import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.userpreferences.domain.aggregate.UserPreferencesAggregate
import io.github.kamiazya.scopes.userpreferences.domain.entity.UserPreferences
import io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError
import io.github.kamiazya.scopes.userpreferences.domain.value.HierarchyPreferences
import io.github.kamiazya.scopes.userpreferences.infrastructure.config.UserPreferencesConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class FileBasedUserPreferencesRepositoryTest :
    DescribeSpec({

        beforeEach {
            clearAllMocks()
        }

        afterEach {
            // Clean up test directories
            try {
                Path("test-config").deleteRecursively()
            } catch (_: Exception) {
                // Ignore cleanup errors
            }
        }

        describe("FileBasedUserPreferencesRepository") {
            val mockLogger = mockk<Logger>(relaxed = true)
            val mockClock = mockk<Clock>()
            val fixedInstant = Instant.fromEpochSeconds(1640995200)
            val testConfigPath = "test-config"

            beforeEach {
                every { mockClock.now() } returns fixedInstant
            }

            describe("initialization") {
                it("should create config directory if it doesn't exist") {
                    // Given - directory doesn't exist
                    val configPath = Path("test-config-init")
                    configPath.deleteRecursively()

                    // When
                    FileBasedUserPreferencesRepository("test-config-init", mockLogger, mockClock)

                    // Then
                    configPath.exists() shouldBe true

                    // Cleanup
                    configPath.deleteRecursively()
                }

                it("should not fail if config directory already exists") {
                    // Given - directory already exists
                    val configPath = Path("test-config-existing")
                    configPath.toFile().mkdirs()

                    // When - should not throw
                    FileBasedUserPreferencesRepository("test-config-existing", mockLogger, mockClock)

                    // Then - directory still exists
                    configPath.exists() shouldBe true

                    // Cleanup
                    configPath.deleteRecursively()
                }
            }

            describe("saving preferences") {
                it("should save valid preferences to file successfully") {
                    // Given
                    val repository = FileBasedUserPreferencesRepository(testConfigPath, mockLogger, mockClock)

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

                    // When
                    val result = runBlocking { repository.save(aggregate) }

                    // Then
                    result.isRight() shouldBe true

                    // Verify file was created and contains correct JSON
                    val configFile = Path(testConfigPath, UserPreferencesConfig.CONFIG_FILE_NAME)
                    configFile.exists() shouldBe true

                    val jsonContent = configFile.readText()
                    jsonContent shouldContain "\"version\": 1"
                    jsonContent shouldContain "\"maxDepth\": 10"
                    jsonContent shouldContain "\"maxChildrenPerScope\": 20"

                    verify { mockLogger.info("Saved user preferences to $configFile") }
                }

                it("should save preferences with null values (unlimited settings)") {
                    // Given
                    val repository = FileBasedUserPreferencesRepository(testConfigPath, mockLogger, mockClock)

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

                    // When
                    val result = runBlocking { repository.save(aggregate) }

                    // Then
                    result.isRight() shouldBe true

                    val configFile = Path(testConfigPath, UserPreferencesConfig.CONFIG_FILE_NAME)
                    val jsonContent = configFile.readText()
                    jsonContent shouldContain "\"maxDepth\": null"
                    jsonContent shouldContain "\"maxChildrenPerScope\": null"
                }

                it("should save preferences with mixed null and valid values") {
                    // Given
                    val repository = FileBasedUserPreferencesRepository(testConfigPath, mockLogger, mockClock)

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

                    // When
                    val result = runBlocking { repository.save(aggregate) }

                    // Then
                    result.isRight() shouldBe true

                    val configFile = Path(testConfigPath, UserPreferencesConfig.CONFIG_FILE_NAME)
                    val jsonContent = configFile.readText()
                    jsonContent shouldContain "\"maxDepth\": 15"
                    jsonContent shouldContain "\"maxChildrenPerScope\": null"
                }

                it("should fail when aggregate has no preferences") {
                    // Given
                    val repository = FileBasedUserPreferencesRepository(testConfigPath, mockLogger, mockClock)

                    val aggregate = UserPreferencesAggregate(
                        id = AggregateId.Simple.generate(),
                        version = AggregateVersion.initial(),
                        preferences = null, // No preferences
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant,
                    )

                    // When
                    val result = runBlocking { repository.save(aggregate) }

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error shouldBe UserPreferencesError.PreferencesNotInitialized()
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )
                }

                it("should update cache after successful save") {
                    // Given
                    val repository = FileBasedUserPreferencesRepository(testConfigPath, mockLogger, mockClock)

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

                    // When - save first
                    runBlocking { repository.save(aggregate) }

                    // Then - subsequent findForCurrentUser should return cached value without file I/O
                    val result = runBlocking { repository.findForCurrentUser() }
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { foundAggregate ->
                            foundAggregate!!.preferences!!.hierarchyPreferences.maxDepth shouldBe 5
                            foundAggregate.preferences!!.hierarchyPreferences.maxChildrenPerScope shouldBe 10
                        },
                    )
                }
            }

            describe("finding preferences by current user") {
                it("should return null when no config file exists") {
                    // Given
                    val repository = FileBasedUserPreferencesRepository(testConfigPath, mockLogger, mockClock)

                    // When
                    val result = runBlocking { repository.findForCurrentUser() }

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { aggregate ->
                            aggregate shouldBe null
                        },
                    )

                    verify { mockLogger.debug("No preferences file found at ${Path(testConfigPath, UserPreferencesConfig.CONFIG_FILE_NAME)}") }
                }

                it("should load valid preferences from file successfully") {
                    // Given
                    val repository = FileBasedUserPreferencesRepository(testConfigPath, mockLogger, mockClock)
                    val configFile = Path(testConfigPath, UserPreferencesConfig.CONFIG_FILE_NAME)

                    // Create valid JSON file
                    val validJson = """
                    {
                        "version": 1,
                        "hierarchyPreferences": {
                            "maxDepth": 25,
                            "maxChildrenPerScope": 50
                        }
                    }
                    """.trimIndent()
                    configFile.writeText(validJson)

                    // When
                    val result = runBlocking { repository.findForCurrentUser() }

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { aggregate ->
                            aggregate!!.preferences!!.hierarchyPreferences.maxDepth shouldBe 25
                            aggregate.preferences!!.hierarchyPreferences.maxChildrenPerScope shouldBe 50
                            aggregate.createdAt shouldBe fixedInstant
                            aggregate.updatedAt shouldBe fixedInstant
                        },
                    )

                    verify { mockLogger.info("Loaded user preferences from $configFile") }
                }

                it("should load preferences with null values from file") {
                    // Given
                    val repository = FileBasedUserPreferencesRepository(testConfigPath, mockLogger, mockClock)
                    val configFile = Path(testConfigPath, UserPreferencesConfig.CONFIG_FILE_NAME)

                    val validJson = """
                    {
                        "version": 1,
                        "hierarchyPreferences": {
                            "maxDepth": null,
                            "maxChildrenPerScope": null
                        }
                    }
                    """.trimIndent()
                    configFile.writeText(validJson)

                    // When
                    val result = runBlocking { repository.findForCurrentUser() }

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { aggregate ->
                            aggregate!!.preferences!!.hierarchyPreferences.maxDepth shouldBe null
                            aggregate.preferences!!.hierarchyPreferences.maxChildrenPerScope shouldBe null
                        },
                    )
                }

                it("should handle corrupted JSON file") {
                    // Given
                    val repository = FileBasedUserPreferencesRepository(testConfigPath, mockLogger, mockClock)
                    val configFile = Path(testConfigPath, UserPreferencesConfig.CONFIG_FILE_NAME)

                    // Create invalid JSON
                    configFile.writeText("{ invalid json }")

                    // When
                    val result = runBlocking { repository.findForCurrentUser() }

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error.shouldBeInstanceOf<UserPreferencesError.InvalidPreferenceValue>()
                            val invalidError = error as UserPreferencesError.InvalidPreferenceValue
                            invalidError.key shouldBe "load"
                            invalidError.value shouldBe configFile.toString()
                            invalidError.reason shouldContain "Failed to read preferences"
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )

                    verify { mockLogger.error(match<String> { it.contains("Failed to load user preferences") }) }
                }

                it("should handle file with invalid hierarchy preferences") {
                    // Given
                    val repository = FileBasedUserPreferencesRepository(testConfigPath, mockLogger, mockClock)
                    val configFile = Path(testConfigPath, UserPreferencesConfig.CONFIG_FILE_NAME)

                    // Create JSON with invalid values (negative numbers)
                    val invalidJson = """
                    {
                        "version": 1,
                        "hierarchyPreferences": {
                            "maxDepth": -5,
                            "maxChildrenPerScope": 10
                        }
                    }
                    """.trimIndent()
                    configFile.writeText(invalidJson)

                    // When
                    val result = runBlocking { repository.findForCurrentUser() }

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error.shouldBeInstanceOf<UserPreferencesError.InvalidPreferenceValue>()
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )
                }

                it("should cache loaded preferences for subsequent calls") {
                    // Given
                    val repository = FileBasedUserPreferencesRepository(testConfigPath, mockLogger, mockClock)
                    val configFile = Path(testConfigPath, UserPreferencesConfig.CONFIG_FILE_NAME)

                    val validJson = """
                    {
                        "version": 1,
                        "hierarchyPreferences": {
                            "maxDepth": 8,
                            "maxChildrenPerScope": 16
                        }
                    }
                    """.trimIndent()
                    configFile.writeText(validJson)

                    // When - call multiple times
                    val result1 = runBlocking { repository.findForCurrentUser() }
                    val result2 = runBlocking { repository.findForCurrentUser() }

                    // Then - both should succeed with same data
                    result1.isRight() shouldBe true
                    result2.isRight() shouldBe true

                    result1.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { aggregate1 ->
                            result2.fold(
                                { throw AssertionError("Expected Right but got Left: $it") },
                                { aggregate2 ->
                                    aggregate1!!.preferences!!.hierarchyPreferences.maxDepth shouldBe 8
                                    aggregate2!!.preferences!!.hierarchyPreferences.maxDepth shouldBe 8
                                    // Should be the same object due to caching
                                    aggregate1 shouldBe aggregate2
                                },
                            )
                        },
                    )

                    // File should only be loaded once (first call)
                    verify(exactly = 1) { mockLogger.info(match<String> { it.contains("Loaded user preferences from") }) }
                }
            }

            describe("finding preferences by ID") {
                it("should delegate to findForCurrentUser for current user ID") {
                    // Given
                    val repository = FileBasedUserPreferencesRepository(testConfigPath, mockLogger, mockClock)
                    val configFile = Path(testConfigPath, UserPreferencesConfig.CONFIG_FILE_NAME)

                    val validJson = """
                    {
                        "version": 1,
                        "hierarchyPreferences": {
                            "maxDepth": 12,
                            "maxChildrenPerScope": 24
                        }
                    }
                    """.trimIndent()
                    configFile.writeText(validJson)

                    // Load once to get the current user ID from the repository
                    val currentUserResult = runBlocking { repository.findForCurrentUser() }
                    val currentUserId = currentUserResult.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { it!!.id },
                    )

                    // When
                    val result = runBlocking { repository.findById(currentUserId) }

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { aggregate ->
                            aggregate!!.preferences!!.hierarchyPreferences.maxDepth shouldBe 12
                            aggregate.preferences!!.hierarchyPreferences.maxChildrenPerScope shouldBe 24
                        },
                    )
                }

                it("should return null for non-current user IDs") {
                    // Given
                    val repository = FileBasedUserPreferencesRepository(testConfigPath, mockLogger, mockClock)
                    val differentId = AggregateId.Simple.generate()

                    // When
                    val result = runBlocking { repository.findById(differentId) }

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { aggregate ->
                            aggregate shouldBe null
                        },
                    )
                }
            }

            describe("error scenarios and edge cases") {
                it("should handle permission errors when writing file") {
                    // Given - This test is platform-specific and might be hard to simulate
                    // We'll test by using an invalid path that should cause write failure
                    val repository = FileBasedUserPreferencesRepository("/invalid/readonly/path", mockLogger, mockClock)

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

                    // When
                    val result = runBlocking { repository.save(aggregate) }

                    // Then - should handle the I/O error gracefully
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error.shouldBeInstanceOf<UserPreferencesError.InvalidPreferenceValue>()
                            val invalidError = error as UserPreferencesError.InvalidPreferenceValue
                            invalidError.key shouldBe "save"
                            invalidError.reason shouldContain "Failed to write preferences"
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )

                    verify { mockLogger.error(match<String> { it.contains("Failed to save user preferences") }) }
                }

                it("should handle empty JSON file") {
                    // Given
                    val repository = FileBasedUserPreferencesRepository(testConfigPath, mockLogger, mockClock)
                    val configFile = Path(testConfigPath, UserPreferencesConfig.CONFIG_FILE_NAME)
                    configFile.writeText("")

                    // When
                    val result = runBlocking { repository.findForCurrentUser() }

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error.shouldBeInstanceOf<UserPreferencesError.InvalidPreferenceValue>()
                            val invalidError = error as UserPreferencesError.InvalidPreferenceValue
                            invalidError.reason shouldContain "Failed to read preferences"
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )
                }

                it("should handle JSON with missing required fields") {
                    // Given
                    val repository = FileBasedUserPreferencesRepository(testConfigPath, mockLogger, mockClock)
                    val configFile = Path(testConfigPath, UserPreferencesConfig.CONFIG_FILE_NAME)

                    // JSON missing hierarchyPreferences
                    val incompleteJson = """
                    {
                        "version": 1
                    }
                    """.trimIndent()
                    configFile.writeText(incompleteJson)

                    // When
                    val result = runBlocking { repository.findForCurrentUser() }

                    // Then - should work due to default values in config class
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { aggregate ->
                            // Should use defaults (null values)
                            aggregate!!.preferences!!.hierarchyPreferences.maxDepth shouldBe null
                            aggregate.preferences!!.hierarchyPreferences.maxChildrenPerScope shouldBe null
                        },
                    )
                }
            }

            describe("concurrent access scenarios") {
                it("should handle multiple save operations correctly") {
                    // Given
                    val repository = FileBasedUserPreferencesRepository(testConfigPath, mockLogger, mockClock)

                    val hierarchyPreferences1 = HierarchyPreferences.create(5, 10).getOrNull()!!
                    val hierarchyPreferences2 = HierarchyPreferences.create(15, 20).getOrNull()!!

                    val userPreferences1 = UserPreferences(hierarchyPreferences1, fixedInstant, fixedInstant)
                    val userPreferences2 = UserPreferences(hierarchyPreferences2, fixedInstant, fixedInstant)

                    val aggregate1 = UserPreferencesAggregate(
                        id = AggregateId.Simple.generate(),
                        version = AggregateVersion.initial(),
                        preferences = userPreferences1,
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant,
                    )
                    val aggregate2 = UserPreferencesAggregate(
                        id = AggregateId.Simple.generate(),
                        version = AggregateVersion.initial(),
                        preferences = userPreferences2,
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant,
                    )

                    // When - save two different aggregates
                    val result1 = runBlocking { repository.save(aggregate1) }
                    val result2 = runBlocking { repository.save(aggregate2) }

                    // Then - both should succeed, last one should win
                    result1.isRight() shouldBe true
                    result2.isRight() shouldBe true

                    // Verify the last save is what's in the file
                    val finalResult = runBlocking { repository.findForCurrentUser() }
                    finalResult.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { aggregate ->
                            // Should have the second aggregate's values
                            aggregate!!.preferences!!.hierarchyPreferences.maxDepth shouldBe 15
                            aggregate.preferences!!.hierarchyPreferences.maxChildrenPerScope shouldBe 20
                        },
                    )
                }
            }
        }
    })
