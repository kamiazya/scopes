package io.github.kamiazya.scopes.userpreferences.infrastructure.adapters

import io.github.kamiazya.scopes.contracts.userpreferences.errors.UserPreferencesContractError
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
class ErrorMapper(private val logger: Logger) {

    /**
     * Maps a domain error to a contract error.
     *
     * @param domainError The domain error to map
     * @return The corresponding contract error
     */
    fun mapToContractError(domainError: UserPreferencesError): UserPreferencesContractError = when (domainError) {
        is UserPreferencesError.InvalidPreferenceValue -> {
            UserPreferencesContractError.DataError.PreferencesCorrupted(
                details = "Invalid value '${domainError.value}' for preference '${domainError.key}': ${domainError.reason}",
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
            UserPreferencesContractError.DataError.PreferencesCorrupted(
                details = domainError.reason,
                configPath = null,
            )
        }

        is UserPreferencesError.InvalidHierarchyPreferences -> {
            UserPreferencesContractError.DataError.PreferencesCorrupted(
                details = domainError.reason,
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

        // Default mapping for any unmapped errors
        else -> {
            logger.error(
                "Unmapped UserPreferencesError encountered, mapping to PreferencesCorrupted",
                mapOf(
                    "errorClass" to domainError::class.simpleName.orEmpty(),
                    "errorMessage" to (domainError.message ?: "No message provided"),
                    "errorType" to domainError::class.qualifiedName.orEmpty(),
                ),
            )
            UserPreferencesContractError.DataError.PreferencesCorrupted(
                details = "Unexpected error occurred",
                configPath = null,
            )
        }
    }
}
