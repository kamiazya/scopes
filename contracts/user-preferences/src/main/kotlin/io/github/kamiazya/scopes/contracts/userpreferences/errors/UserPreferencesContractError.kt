package io.github.kamiazya.scopes.contracts.userpreferences.errors

/**
 * Sealed interface representing all possible errors in the User Preferences contract layer.
 *
 * DESIGN PRINCIPLE: Zero-Configuration Start
 * PreferencesNotInitialized error is intentionally NOT included to enforce
 * the principle that the system MUST work without any initial configuration.
 * If preferences are not found, default values should be used instead of throwing errors.
 */
public sealed interface UserPreferencesContractError {

    /**
     * Errors related to invalid input data.
     */
    public sealed interface InputError : UserPreferencesContractError {
        /**
         * Invalid preference key.
         */
        public data class InvalidPreferenceKey(public val key: String) : InputError
    }

    /**
     * Errors related to data integrity issues.
     */
    public sealed interface DataError : UserPreferencesContractError {
        /**
         * The preferences data is corrupted or invalid.
         * Note: details and configPath are kept for internal logging/telemetry only.
         * They are NOT included in the public message for security reasons.
         */
        public data class PreferencesCorrupted(public val details: String, public val configPath: String? = null) : DataError

        /**
         * The preferences format requires migration to a newer version.
         */
        public data class PreferencesMigrationRequired(public val fromVersion: String, public val toVersion: String) : DataError
    }
}
