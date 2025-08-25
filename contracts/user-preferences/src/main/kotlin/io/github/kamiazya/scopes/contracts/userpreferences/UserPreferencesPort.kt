package io.github.kamiazya.scopes.contracts.userpreferences

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.userpreferences.errors.UserPreferencesContractError
import io.github.kamiazya.scopes.contracts.userpreferences.queries.GetPreferenceQuery
import io.github.kamiazya.scopes.contracts.userpreferences.results.PreferenceResult

/**
 * Public contract for user preferences operations.
 * Currently focused on read-only operations for hierarchy preferences.
 *
 * DESIGN PRINCIPLE: Zero-Configuration Start
 * Implementations MUST return default preferences when no configuration exists,
 * rather than returning an error. Users should be able to start using the tool
 * immediately without any setup.
 *
 * NULL SEMANTICS: In preference values, null means "unlimited" or "no limit".
 * For example, maxDepth = null means unlimited depth is allowed.
 */
public interface UserPreferencesPort {
    /**
     * Retrieves user preferences.
     *
     * Implementations MUST return default preference values when no stored
     * configuration exists rather than returning NotFound-style errors.
     * This ensures the zero-configuration principle is maintained.
     *
     * @param query The query specifying which preferences to retrieve
     * @return Either an error or the preference result (never null - defaults if not set)
     */
    public suspend fun getPreference(query: GetPreferenceQuery): Either<UserPreferencesContractError, PreferenceResult>
}
