package io.github.kamiazya.scopes.scopemanagement.domain.error

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
     * Alias not found error.
     */
    data class AliasNotFound(override val occurredAt: Instant, val aliasName: String) : ScopeAliasError()

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
     * Data inconsistency error where alias exists but the referenced scope doesn't exist.
     * This indicates a serious data integrity issue that needs attention.
     */
    data class DataInconsistency(override val occurredAt: Instant, val aliasName: String, val scopeId: ScopeId) : ScopeAliasError()
}
