package io.github.kamiazya.scopes.interfaces.shared.adapters

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import io.github.kamiazya.scopes.interfaces.shared.services.UserPreferencesService
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.port.HierarchyPolicyProvider
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
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
        val hierarchyPolicy = userPreferencesService.getCurrentUserPreferences()
            .fold(
                // On error, log and use default policy
                ifLeft = { serviceError ->
                    logger.warn(
                        "Failed to fetch user preferences, using defaults",
                        mapOf(
                            "error" to serviceError.message,
                            "errorType" to (serviceError::class.simpleName ?: "Unknown"),
                        ),
                    )
                    HierarchyPolicy.default()
                },
                // On success, translate to domain model
                ifRight = { dto ->
                    // Use user preferences if set, otherwise use system defaults
                    HierarchyPolicy.create(
                        maxDepth = dto.hierarchyPreferences.maxDepth,
                        maxChildrenPerScope = dto.hierarchyPreferences.maxChildrenPerScope,
                    ).getOrElse { error ->
                        logger.error(
                            "Invalid hierarchy preferences from user preferences, using defaults",
                            mapOf(
                                "maxDepth" to (dto.hierarchyPreferences.maxDepth?.toString() ?: "not set"),
                                "maxChildrenPerScope" to (dto.hierarchyPreferences.maxChildrenPerScope?.toString() ?: "not set"),
                                "error" to error.toString(),
                            ),
                        )
                        HierarchyPolicy.default()
                    }
                },
            )

        logger.debug(
            "Hierarchy policy resolved",
            mapOf(
                "maxDepth" to (hierarchyPolicy.maxDepth?.toString() ?: "unlimited"),
                "maxChildrenPerScope" to (hierarchyPolicy.maxChildrenPerScope?.toString() ?: "unlimited"),
            ),
        )

        hierarchyPolicy
    }
}
