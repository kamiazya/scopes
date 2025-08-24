package io.github.kamiazya.scopes.userpreferences.infrastructure.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.userpreferences.UserPreferencesPort
import io.github.kamiazya.scopes.contracts.userpreferences.errors.UserPreferencesContractError
import io.github.kamiazya.scopes.contracts.userpreferences.queries.GetPreferenceQuery
import io.github.kamiazya.scopes.contracts.userpreferences.results.PreferenceResult
import io.github.kamiazya.scopes.userpreferences.application.handler.GetCurrentUserPreferencesHandler
import io.github.kamiazya.scopes.userpreferences.application.query.GetCurrentUserPreferences

/**
 * Adapter implementation of UserPreferencesPort.
 *
 * This adapter delegates to application handlers without using transactions
 * since all operations are read-only. It follows the Zero-Configuration Start
 * principle by always returning default values when preferences don't exist.
 */
class UserPreferencesPortAdapter(private val getCurrentUserPreferencesHandler: GetCurrentUserPreferencesHandler, private val errorMapper: ErrorMapper) :
    UserPreferencesPort {

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
