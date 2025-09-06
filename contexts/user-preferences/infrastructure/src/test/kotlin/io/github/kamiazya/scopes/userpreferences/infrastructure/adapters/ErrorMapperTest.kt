package io.github.kamiazya.scopes.userpreferences.infrastructure.adapters

import io.github.kamiazya.scopes.contracts.userpreferences.errors.UserPreferencesContractError
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk

class ErrorMapperTest :
    DescribeSpec({
        val mockLogger = mockk<Logger>(relaxed = true)
        val errorMapper = ErrorMapper(mockLogger)

        describe("ErrorMapper") {
            context("InvalidPreferenceValue errors") {
                it("should map INVALID_TYPE to PreferencesCorrupted") {
                    val domainError = UserPreferencesError.InvalidPreferenceValue("theme", "invalid_theme", UserPreferencesError.ValidationError.INVALID_TYPE)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<UserPreferencesContractError.DataError.PreferencesCorrupted>()
                    contractError as UserPreferencesContractError.DataError.PreferencesCorrupted
                    contractError.details shouldBe "Invalid value 'invalid_theme' for preference 'theme': Invalid type"
                    contractError.configPath shouldBe null
                }

                it("should map OUT_OF_RANGE to PreferencesCorrupted") {
                    val domainError = UserPreferencesError.InvalidPreferenceValue("fontSize", "999", UserPreferencesError.ValidationError.OUT_OF_RANGE)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<UserPreferencesContractError.DataError.PreferencesCorrupted>()
                    contractError as UserPreferencesContractError.DataError.PreferencesCorrupted
                    contractError.details shouldBe "Invalid value '999' for preference 'fontSize': Value out of range"
                    contractError.configPath shouldBe null
                }

                it("should map INVALID_FORMAT to PreferencesCorrupted") {
                    val domainError = UserPreferencesError.InvalidPreferenceValue("color", "not-a-color", UserPreferencesError.ValidationError.INVALID_FORMAT)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<UserPreferencesContractError.DataError.PreferencesCorrupted>()
                    contractError as UserPreferencesContractError.DataError.PreferencesCorrupted
                    contractError.details shouldBe "Invalid value 'not-a-color' for preference 'color': Invalid format"
                    contractError.configPath shouldBe null
                }

                it("should map UNSUPPORTED_VALUE to PreferencesCorrupted") {
                    val domainError = UserPreferencesError.InvalidPreferenceValue("mode", "unsupported", UserPreferencesError.ValidationError.UNSUPPORTED_VALUE)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<UserPreferencesContractError.DataError.PreferencesCorrupted>()
                    contractError as UserPreferencesContractError.DataError.PreferencesCorrupted
                    contractError.details shouldBe "Invalid value 'unsupported' for preference 'mode': Unsupported value"
                    contractError.configPath shouldBe null
                }
            }

            context("PreferenceNotFound errors") {
                it("should map to InvalidPreferenceKey") {
                    val domainError = UserPreferencesError.PreferenceNotFound("nonexistentKey")
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<UserPreferencesContractError.InputError.InvalidPreferenceKey>()
                    contractError as UserPreferencesContractError.InputError.InvalidPreferenceKey
                    contractError.key shouldBe "nonexistentKey"
                }
            }

            context("InvalidHierarchySettings errors") {
                it("should map INVALID_DEPTH to PreferencesCorrupted") {
                    val domainError = UserPreferencesError.InvalidHierarchySettings(UserPreferencesError.HierarchySettingType.INVALID_DEPTH)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<UserPreferencesContractError.DataError.PreferencesCorrupted>()
                    contractError as UserPreferencesContractError.DataError.PreferencesCorrupted
                    contractError.details shouldBe "Invalid hierarchy settings: Invalid hierarchy depth"
                    contractError.configPath shouldBe null
                }

                it("should map CIRCULAR_REFERENCE to PreferencesCorrupted") {
                    val domainError = UserPreferencesError.InvalidHierarchySettings(UserPreferencesError.HierarchySettingType.CIRCULAR_REFERENCE)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<UserPreferencesContractError.DataError.PreferencesCorrupted>()
                    contractError as UserPreferencesContractError.DataError.PreferencesCorrupted
                    contractError.details shouldBe "Invalid hierarchy settings: Circular reference detected"
                    contractError.configPath shouldBe null
                }

                it("should map ORPHANED_NODE to PreferencesCorrupted") {
                    val domainError = UserPreferencesError.InvalidHierarchySettings(UserPreferencesError.HierarchySettingType.ORPHANED_NODE)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<UserPreferencesContractError.DataError.PreferencesCorrupted>()
                    contractError as UserPreferencesContractError.DataError.PreferencesCorrupted
                    contractError.details shouldBe "Invalid hierarchy settings: Orphaned node found"
                    contractError.configPath shouldBe null
                }

                it("should map DUPLICATE_PATH to PreferencesCorrupted") {
                    val domainError = UserPreferencesError.InvalidHierarchySettings(UserPreferencesError.HierarchySettingType.DUPLICATE_PATH)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<UserPreferencesContractError.DataError.PreferencesCorrupted>()
                    contractError as UserPreferencesContractError.DataError.PreferencesCorrupted
                    contractError.details shouldBe "Invalid hierarchy settings: Duplicate path detected"
                    contractError.configPath shouldBe null
                }
            }

            context("InvalidHierarchyPreferences errors") {
                it("should map INVALID_DEFAULT to PreferencesCorrupted") {
                    val domainError = UserPreferencesError.InvalidHierarchyPreferences(UserPreferencesError.HierarchyPreferenceType.INVALID_DEFAULT)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<UserPreferencesContractError.DataError.PreferencesCorrupted>()
                    contractError as UserPreferencesContractError.DataError.PreferencesCorrupted
                    contractError.details shouldBe "Invalid hierarchy preferences: Invalid default value"
                    contractError.configPath shouldBe null
                }

                it("should map CONFLICTING_RULES to PreferencesCorrupted") {
                    val domainError = UserPreferencesError.InvalidHierarchyPreferences(UserPreferencesError.HierarchyPreferenceType.CONFLICTING_RULES)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<UserPreferencesContractError.DataError.PreferencesCorrupted>()
                    contractError as UserPreferencesContractError.DataError.PreferencesCorrupted
                    contractError.details shouldBe "Invalid hierarchy preferences: Conflicting rules"
                    contractError.configPath shouldBe null
                }

                it("should map MISSING_REQUIRED to PreferencesCorrupted") {
                    val domainError = UserPreferencesError.InvalidHierarchyPreferences(UserPreferencesError.HierarchyPreferenceType.MISSING_REQUIRED)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<UserPreferencesContractError.DataError.PreferencesCorrupted>()
                    contractError as UserPreferencesContractError.DataError.PreferencesCorrupted
                    contractError.details shouldBe "Invalid hierarchy preferences: Missing required preference"
                    contractError.configPath shouldBe null
                }

                it("should map INVALID_INHERITANCE to PreferencesCorrupted") {
                    val domainError = UserPreferencesError.InvalidHierarchyPreferences(UserPreferencesError.HierarchyPreferenceType.INVALID_INHERITANCE)
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<UserPreferencesContractError.DataError.PreferencesCorrupted>()
                    contractError as UserPreferencesContractError.DataError.PreferencesCorrupted
                    contractError.details shouldBe "Invalid hierarchy preferences: Invalid inheritance"
                    contractError.configPath shouldBe null
                }
            }

            context("PreferencesNotInitialized errors") {
                it("should map to PreferencesCorrupted with appropriate message") {
                    val domainError = UserPreferencesError.PreferencesNotInitialized
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<UserPreferencesContractError.DataError.PreferencesCorrupted>()
                    contractError as UserPreferencesContractError.DataError.PreferencesCorrupted
                    contractError.details shouldBe "Preferences not initialized (system should use defaults)"
                    contractError.configPath shouldBe null
                }
            }

            context("PreferencesAlreadyInitialized errors") {
                it("should map to PreferencesCorrupted with appropriate message") {
                    val domainError = UserPreferencesError.PreferencesAlreadyInitialized
                    val contractError = errorMapper.mapToContractError(domainError)

                    contractError.shouldBeInstanceOf<UserPreferencesContractError.DataError.PreferencesCorrupted>()
                    contractError as UserPreferencesContractError.DataError.PreferencesCorrupted
                    contractError.details shouldBe "Preferences already initialized"
                    contractError.configPath shouldBe null
                }
            }
        }
    })
