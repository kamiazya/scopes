package io.github.kamiazya.scopes.scopemanagement.domain.error

import kotlin.time.Duration

/**
 * Errors specific to integrating with the User Preferences context
 * from the Scope Management domain perspective.
 *
 * These errors represent failures when retrieving hierarchy policies
 * from the User Preferences bounded context.
 */
sealed class UserPreferencesIntegrationError : ScopesError() {

    /**
     * Error when user preferences service is unreachable.
     */
    data class ServiceUnavailable(val retryAfter: Duration? = null) : UserPreferencesIntegrationError()

    /**
     * Error when hierarchy settings are missing from user preferences.
     */
    data object HierarchySettingsNotFound : UserPreferencesIntegrationError()

    /**
     * Error when hierarchy settings contain invalid values.
     */
    data class InvalidHierarchySettings(val maxDepth: Int? = null, val maxChildrenPerScope: Int? = null, val validationErrors: List<String>) :
        UserPreferencesIntegrationError()

    /**
     * Error when preferences service returns malformed data.
     */
    data object MalformedResponse : UserPreferencesIntegrationError()

    /**
     * Error when preferences service request times out.
     */
    data class RequestTimeout(val timeoutDuration: Duration) : UserPreferencesIntegrationError()
}
