package io.github.kamiazya.scopes.userpreferences.infrastructure.adapters

import io.github.kamiazya.scopes.contracts.userpreferences.errors.UserPreferencesContractError
import io.github.kamiazya.scopes.platform.application.error.BaseErrorMapper
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError

/**
 * Maps domain errors to contract layer errors.
 *
 * This mapper ensures that domain-specific errors are properly translated
 * to contract errors that can be exposed to external consumers.
 *
 * NOTE: PreferencesNotInitialized should never occur in practice since
 * the system follows the Zero-Configuration Start principle and always
 * returns default values when preferences don't exist.
 */
class ErrorMapper(logger: Logger) : BaseErrorMapper<UserPreferencesError, UserPreferencesContractError>(logger) {

    override fun mapToContractError(domainError: UserPreferencesError): UserPreferencesContractError = when (domainError) {
        is UserPreferencesError.InvalidPreferenceValue -> {
            val validationDetails = when (domainError.validationError) {
                UserPreferencesError.ValidationError.INVALID_TYPE ->
                    "Invalid type"
                UserPreferencesError.ValidationError.OUT_OF_RANGE ->
                    "Value out of range"
                UserPreferencesError.ValidationError.INVALID_FORMAT ->
                    "Invalid format"
                UserPreferencesError.ValidationError.UNSUPPORTED_VALUE ->
                    "Unsupported value"
            }
            UserPreferencesContractError.DataError.PreferencesCorrupted(
                details = "Invalid value '${domainError.value}' for preference '${domainError.key}': $validationDetails",
                configPath = null,
            )
        }

        is UserPreferencesError.PreferenceNotFound -> {
            // This should be handled by returning default values, but if it occurs...
            UserPreferencesContractError.InputError.InvalidPreferenceKey(
                key = domainError.key,
            )
        }

        is UserPreferencesError.InvalidHierarchySettings -> {
            val settingDetails = when (domainError.settingType) {
                UserPreferencesError.HierarchySettingType.INVALID_DEPTH -> "Invalid hierarchy depth"
                UserPreferencesError.HierarchySettingType.CIRCULAR_REFERENCE -> "Circular reference detected"
                UserPreferencesError.HierarchySettingType.ORPHANED_NODE -> "Orphaned node found"
                UserPreferencesError.HierarchySettingType.DUPLICATE_PATH -> "Duplicate path detected"
            }
            UserPreferencesContractError.DataError.PreferencesCorrupted(
                details = "Invalid hierarchy settings: $settingDetails",
                configPath = null,
            )
        }

        is UserPreferencesError.InvalidHierarchyPreferences -> {
            val preferenceDetails = when (domainError.preferenceType) {
                UserPreferencesError.HierarchyPreferenceType.INVALID_DEFAULT -> "Invalid default value"
                UserPreferencesError.HierarchyPreferenceType.CONFLICTING_RULES -> "Conflicting rules"
                UserPreferencesError.HierarchyPreferenceType.MISSING_REQUIRED -> "Missing required preference"
                UserPreferencesError.HierarchyPreferenceType.INVALID_INHERITANCE -> "Invalid inheritance"
            }
            UserPreferencesContractError.DataError.PreferencesCorrupted(
                details = "Invalid hierarchy preferences: $preferenceDetails",
                configPath = null,
            )
        }

        is UserPreferencesError.PreferencesNotInitialized -> {
            // This should never happen in practice due to Zero-Configuration Start principle
            // The handler should create default preferences instead
            UserPreferencesContractError.DataError.PreferencesCorrupted(
                details = "Preferences not initialized (system should use defaults)",
                configPath = null,
            )
        }

        is UserPreferencesError.PreferencesAlreadyInitialized -> {
            // This is an internal error that shouldn't be exposed to external consumers
            UserPreferencesContractError.DataError.PreferencesCorrupted(
                details = "Preferences already initialized",
                configPath = null,
            )
        }
    }
}
