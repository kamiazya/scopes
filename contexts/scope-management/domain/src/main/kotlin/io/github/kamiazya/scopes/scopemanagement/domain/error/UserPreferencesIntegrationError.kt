package io.github.kamiazya.scopes.scopemanagement.domain.error

import kotlinx.datetime.Instant
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
    data class PreferencesServiceUnavailable(override val occurredAt: Instant, val retryAfter: Duration? = null) : UserPreferencesIntegrationError()

    /**
     * Error when hierarchy settings are missing from user preferences.
     */
    data class HierarchySettingsNotFound(override val occurredAt: Instant) : UserPreferencesIntegrationError()

    /**
     * Error when hierarchy settings contain invalid values.
     */
    data class InvalidHierarchySettings(
        override val occurredAt: Instant,
        val maxDepth: Int? = null,
        val maxChildrenPerScope: Int? = null,
        val validationErrors: List<String>,
    ) : UserPreferencesIntegrationError()

    /**
     * Error when preferences service returns malformed data.
     */
    data class MalformedPreferencesResponse(override val occurredAt: Instant, val expectedFormat: String, val actualContent: String? = null) :
        UserPreferencesIntegrationError()

    /**
     * Error when preferences service request times out.
     */
    data class PreferencesRequestTimeout(override val occurredAt: Instant, val timeout: Duration) : UserPreferencesIntegrationError()
}
