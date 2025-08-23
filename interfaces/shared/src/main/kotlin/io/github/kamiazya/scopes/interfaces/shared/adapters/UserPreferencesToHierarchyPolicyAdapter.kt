package io.github.kamiazya.scopes.interfaces.shared.adapters

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.interfaces.shared.error.UserPreferencesServiceError
import io.github.kamiazya.scopes.interfaces.shared.services.UserPreferencesService
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.port.HierarchyPolicyProvider
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.error.UserPreferencesIntegrationError
import io.github.kamiazya.scopes.scopemanagement.domain.error.currentTimestamp
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.HierarchyPolicy
/**
 * Adapter that implements the Anti-Corruption Layer pattern.
 *
 * Translates between the User Preferences context and the Scope Management context
 * for hierarchy-related settings only.
 */
class UserPreferencesToHierarchyPolicyAdapter(private val userPreferencesService: UserPreferencesService, private val logger: Logger) :
    HierarchyPolicyProvider {

    override suspend fun getPolicy(): Either<ScopesError, HierarchyPolicy> = either {
        logger.debug("Fetching hierarchy policy from user preferences")

        // Try to get user preferences from external service
        val userPreferences = userPreferencesService.getCurrentUserPreferences()
            .mapLeft { serviceError ->
                logger.warn(
                    "Failed to fetch user preferences, using defaults",
                    mapOf(
                        "error" to serviceError.message,
                        "errorType" to (serviceError::class.simpleName ?: "Unknown"),
                    ),
                )
                // Map service error to domain-specific error
                when (serviceError) {
                    is UserPreferencesServiceError.PreferencesCorrupted ->
                        UserPreferencesIntegrationError.MalformedPreferencesResponse(
                            occurredAt = currentTimestamp(),
                            expectedFormat = "Valid hierarchy settings",
                            actualContent = serviceError.details,
                        )
                    is UserPreferencesServiceError.PreferencesReadError ->
                        UserPreferencesIntegrationError.PreferencesServiceUnavailable(
                            occurredAt = currentTimestamp(),
                        )
                    is UserPreferencesServiceError.PreferencesWriteError ->
                        UserPreferencesIntegrationError.PreferencesServiceUnavailable(
                            occurredAt = currentTimestamp(),
                        )
                    is UserPreferencesServiceError.PreferencesMigrationRequired ->
                        UserPreferencesIntegrationError.MalformedPreferencesResponse(
                            occurredAt = currentTimestamp(),
                            expectedFormat = "Version ${serviceError.toVersion} preferences format",
                            actualContent = "Found version ${serviceError.fromVersion}, migration required",
                        )
                }
            }
            .fold(
                // On error, use default policy
                ifLeft = {
                    logger.info("Using default hierarchy policy due to external service error")
                    HierarchyPolicy.default()
                },
                // On success, translate to domain model
                ifRight = { dto ->
                    HierarchyPolicy.create(
                        maxDepth = dto.hierarchySettings.maxDepth,
                        maxChildrenPerScope = dto.hierarchySettings.maxChildrenPerScope,
                    ).fold(
                        ifLeft = { error ->
                            logger.error(
                                "Invalid hierarchy settings from user preferences, using defaults",
                                mapOf(
                                    "maxDepth" to (dto.hierarchySettings.maxDepth?.toString() ?: "unlimited"),
                                    "maxChildrenPerScope" to (dto.hierarchySettings.maxChildrenPerScope?.toString() ?: "unlimited"),
                                    "error" to error.toString(),
                                ),
                            )
                            HierarchyPolicy.default()
                        },
                        ifRight = { it },
                    )
                },
            )

        logger.debug(
            "Hierarchy policy resolved",
            mapOf(
                "maxDepth" to (userPreferences.maxDepth?.toString() ?: "unlimited"),
                "maxChildrenPerScope" to (userPreferences.maxChildrenPerScope?.toString() ?: "unlimited"),
            ),
        )

        userPreferences
    }
}
