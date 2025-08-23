package io.github.kamiazya.scopes.interfaces.shared.error

/**
 * Domain-specific errors for the User Preferences service.
 *
 * These errors represent failures when working with local user preferences,
 * such as configuration files and settings in a local-first tool.
 *
 * DESIGN PRINCIPLE: Zero-Configuration Start
 * The system MUST work without any initial configuration. If preferences
 * are not found, default values should be used instead of throwing errors.
 * PreferencesNotInitialized error is intentionally NOT included to enforce
 * this principle throughout the codebase.
 */
sealed class UserPreferencesServiceError(open val message: String) {

    /**
     * The preferences data is corrupted or invalid.
     */
    data class PreferencesCorrupted(val details: String, val configPath: String? = null) :
        UserPreferencesServiceError("Preferences data is corrupted${configPath?.let { " at $it" } ?: ""}: $details")

    /**
     * Error reading preferences from storage.
     */
    data class PreferencesReadError(val cause: String, val configPath: String? = null) :
        UserPreferencesServiceError("Failed to read preferences${configPath?.let { " from $it" } ?: ""}: $cause")

    /**
     * Error writing preferences to storage.
     */
    data class PreferencesWriteError(val cause: String, val configPath: String? = null) :
        UserPreferencesServiceError("Failed to write preferences${configPath?.let { " to $it" } ?: ""}: $cause")

    /**
     * The preferences format requires migration to a newer version.
     */
    data class PreferencesMigrationRequired(val fromVersion: String, val toVersion: String) :
        UserPreferencesServiceError("Preferences migration required from version $fromVersion to $toVersion")
}
