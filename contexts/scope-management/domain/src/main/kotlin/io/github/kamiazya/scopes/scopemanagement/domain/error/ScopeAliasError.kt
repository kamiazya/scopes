package io.github.kamiazya.scopes.scopemanagement.domain.error

import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Instant

/**
 * Errors related to Scope Alias operations.
 */
sealed class ScopeAliasError : ScopesError() {

    /**
     * Duplicate alias error.
     */
    data class DuplicateAlias(override val occurredAt: Instant, val aliasName: String, val existingScopeId: ScopeId, val attemptedScopeId: ScopeId) :
        ScopeAliasError()

    /**
     * Alias not found by name error.
     * Raised when searching for an alias by its human-readable name.
     */
    data class AliasNotFound(override val occurredAt: Instant, val aliasName: String) : ScopeAliasError()

    /**
     * Alias not found by ID error.
     * Raised when searching for an alias by its unique identifier (ULID).
     */
    data class AliasNotFoundById(override val occurredAt: Instant, val aliasId: AliasId) : ScopeAliasError()

    /**
     * Cannot remove canonical alias error.
     */
    data class CannotRemoveCanonicalAlias(override val occurredAt: Instant, val scopeId: ScopeId, val aliasName: String) : ScopeAliasError()

    /**
     * Alias generation failed error.
     */
    data class AliasGenerationFailed(override val occurredAt: Instant, val scopeId: ScopeId, val retryCount: Int) : ScopeAliasError()

    /**
     * Alias validation failed during generation.
     */
    data class AliasGenerationValidationFailed(override val occurredAt: Instant, val scopeId: ScopeId, val reason: String, val attemptedValue: String) :
        ScopeAliasError()

    /**
     * Data inconsistency errors indicating serious data integrity issues that need attention.
     * These errors represent states where the data relationships are broken or invalid.
     */
    sealed class DataInconsistencyError : ScopeAliasError() {
        /**
         * Alias exists in the alias repository but the referenced scope doesn't exist in the scope repository.
         * This indicates either a failed deletion cascade or corruption in the data store.
         */
        data class AliasExistsButScopeNotFound(override val occurredAt: Instant, val aliasName: String, val scopeId: ScopeId) : DataInconsistencyError()

        // Future data inconsistency patterns can be added here as needed:
        // data class CircularAliasReference(...) : DataInconsistencyError()
        // data class OrphanedCanonicalAlias(...) : DataInconsistencyError()
        // data class DuplicateCanonicalAliases(...) : DataInconsistencyError()
    }
}
