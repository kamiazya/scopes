package io.github.kamiazya.scopes.userpreferences.domain.error

sealed class UserPreferencesError(open val message: String) {
    data class InvalidPreferenceValue(val key: String, val value: String, val reason: String) :
        UserPreferencesError("Invalid value '$value' for preference '$key': $reason")

    data class PreferenceNotFound(val key: String) : UserPreferencesError("Preference with key '$key' not found")

    data class InvalidHierarchySettings(val reason: String) : UserPreferencesError("Invalid hierarchy settings: $reason")

    data class PreferencesNotInitialized(override val message: String = "User preferences have not been initialized") : UserPreferencesError(message)

    data class PreferencesAlreadyInitialized(override val message: String = "User preferences have already been initialized") : UserPreferencesError(message)
}
