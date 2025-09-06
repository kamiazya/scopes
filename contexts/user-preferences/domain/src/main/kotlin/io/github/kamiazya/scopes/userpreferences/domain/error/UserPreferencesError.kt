package io.github.kamiazya.scopes.userpreferences.domain.error

sealed class UserPreferencesError {
    data class InvalidPreferenceValue(val key: String, val value: String, val validationError: ValidationError) : UserPreferencesError()

    data class PreferenceNotFound(val key: String) : UserPreferencesError()

    data class InvalidHierarchySettings(val settingType: HierarchySettingType) : UserPreferencesError()

    data class InvalidHierarchyPreferences(val preferenceType: HierarchyPreferenceType) : UserPreferencesError()

    data object PreferencesNotInitialized : UserPreferencesError()

    data object PreferencesAlreadyInitialized : UserPreferencesError()

    enum class ValidationError {
        INVALID_TYPE,
        OUT_OF_RANGE,
        INVALID_FORMAT,
        UNSUPPORTED_VALUE,
    }

    enum class HierarchySettingType {
        INVALID_DEPTH,
        CIRCULAR_REFERENCE,
        ORPHANED_NODE,
        DUPLICATE_PATH,
    }

    enum class HierarchyPreferenceType {
        INVALID_DEFAULT,
        CONFLICTING_RULES,
        MISSING_REQUIRED,
        INVALID_INHERITANCE,
    }
}
