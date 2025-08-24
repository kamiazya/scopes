package io.github.kamiazya.scopes.contracts.userpreferences.errors

/**
 * Sealed interface representing all possible errors in the User Preferences contract layer.
 *
 * DESIGN PRINCIPLE: Zero-Configuration Start
 * PreferencesNotInitialized error is intentionally NOT included to enforce
 * the principle that the system MUST work without any initial configuration.
 * If preferences are not found, default values should be used instead of throwing errors.
 */
sealed interface UserPreferencesContractError {
    val message: String

    /**
     * Errors related to invalid input data.
     */
    sealed interface InputError : UserPreferencesContractError {
        /**
         * Invalid user ID format.
         */
        data class InvalidUserId(val userId: String, override val message: String = "Invalid user ID format: $userId") : InputError

        /**
         * Invalid preference key.
         */
        data class InvalidPreferenceKey(val key: String, override val message: String = "Invalid preference key: $key") : InputError
    }

    /**
     * Errors related to data integrity issues.
     */
    sealed interface DataError : UserPreferencesContractError {
        /**
         * The preferences data is corrupted or invalid.
         */
        data class PreferencesCorrupted(
            val details: String,
            val configPath: String? = null,
            override val message: String = "Preferences data is corrupted${configPath?.let { " at $it" } ?: ""}: $details",
        ) : DataError

        /**
         * The preferences format requires migration to a newer version.
         */
        data class PreferencesMigrationRequired(
            val fromVersion: String,
            val toVersion: String,
            override val message: String = "Preferences migration required from version $fromVersion to $toVersion",
        ) : DataError
    }

    /**
     * Errors related to system/infrastructure issues.
     */
    sealed interface SystemError : UserPreferencesContractError {
        /**
         * Error reading preferences from storage.
         */
        data class PreferencesReadError(
            val cause: String,
            val configPath: String? = null,
            override val message: String = "Failed to read preferences${configPath?.let { " from $it" } ?: ""}: $cause",
        ) : SystemError

        /**
         * Error writing preferences to storage.
         */
        data class PreferencesWriteError(
            val cause: String,
            val configPath: String? = null,
            override val message: String = "Failed to write preferences${configPath?.let { " to $it" } ?: ""}: $cause",
        ) : SystemError

        /**
         * Service is temporarily unavailable.
         */
        data class ServiceUnavailable(val service: String, override val message: String = "User preferences service unavailable: $service") : SystemError
    }
}
