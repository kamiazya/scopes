package io.github.kamiazya.scopes.userpreferences.infrastructure.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.userpreferences.UserPreferencesQueryPort
import io.github.kamiazya.scopes.contracts.userpreferences.errors.UserPreferencesContractError
import io.github.kamiazya.scopes.contracts.userpreferences.queries.GetPreferenceQuery
import io.github.kamiazya.scopes.contracts.userpreferences.results.PreferenceResult
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.userpreferences.application.handler.query.GetCurrentUserPreferencesHandler
import io.github.kamiazya.scopes.userpreferences.application.query.GetCurrentUserPreferences
import io.github.kamiazya.scopes.userpreferences.infrastructure.adapters.ErrorMapper

/**
 * Adapter implementation of UserPreferencesQueryPort.
 *
 * This adapter delegates to application handlers without using transactions
 * since all operations are read-only. It follows the Zero-Configuration Start
 * principle by always returning default values when preferences don't exist.
 */
class UserPreferencesQueryPortAdapter(
    private val getCurrentUserPreferencesHandler: GetCurrentUserPreferencesHandler,
    private val errorMapper: ErrorMapper,
    private val logger: Logger = ConsoleLogger("UserPreferencesQueryPortAdapter"),
) : UserPreferencesQueryPort {

    override suspend fun getPreference(query: GetPreferenceQuery): Either<UserPreferencesContractError, PreferenceResult> =
        getCurrentUserPreferencesHandler(GetCurrentUserPreferences)
            .mapLeft { domainError ->
                errorMapper.mapToContractError(domainError)
            }
            .map { result ->
                // Map to contract result based on the requested key
                when (query.key) {
                    GetPreferenceQuery.PreferenceKey.HIERARCHY -> {
                        PreferenceResult.HierarchyPreferences(
                            maxDepth = result.hierarchyPreferences.maxDepth,
                            maxChildrenPerScope = result.hierarchyPreferences.maxChildrenPerScope,
                        )
                    }
                }
            }
}
