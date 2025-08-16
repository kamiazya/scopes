package io.github.kamiazya.scopes.domain.entity

import io.github.kamiazya.scopes.domain.valueobject.AliasId
import io.github.kamiazya.scopes.domain.valueobject.AliasName
import io.github.kamiazya.scopes.domain.valueobject.AliasType
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Entity representing a scope alias.
 *
 * Each alias is an alternative identifier for a scope, allowing users to reference
 * scopes using memorable names instead of ULIDs.
 * 
 * The alias has its own unique ID (ULID) for tracking purposes, allowing the alias
 * name to be changed while maintaining identity and audit trail.
 *
 * Business Rules:
 * - Each alias has a unique ID that never changes
 * - Each scope can have exactly one canonical alias
 * - Each scope can have multiple custom aliases
 * - Alias names must be unique across all scopes
 * - Canonical aliases cannot be removed, only replaced
 */
data class ScopeAlias(
    val id: AliasId,  // Unique identifier for this alias
    val scopeId: ScopeId,
    val aliasName: AliasName,
    val aliasType: AliasType,
    val createdAt: Instant,
    val updatedAt: Instant
) {

    companion object {
        /**
         * Creates a new canonical alias for a scope.
         * Generates a new unique ID for the alias.
         */
        fun createCanonical(
            scopeId: ScopeId,
            aliasName: AliasName,
            timestamp: Instant = Clock.System.now()
        ): ScopeAlias = ScopeAlias(
            id = AliasId.generate(),
            scopeId = scopeId,
            aliasName = aliasName,
            aliasType = AliasType.CANONICAL,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        /**
         * Creates a new custom alias for a scope.
         * Generates a new unique ID for the alias.
         */
        fun createCustom(
            scopeId: ScopeId,
            aliasName: AliasName,
            timestamp: Instant = Clock.System.now()
        ): ScopeAlias = ScopeAlias(
            id = AliasId.generate(),
            scopeId = scopeId,
            aliasName = aliasName,
            aliasType = AliasType.CUSTOM,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        /**
         * Creates a new canonical alias with a specific ID.
         * Used when generating deterministic aliases.
         */
        fun createCanonicalWithId(
            id: AliasId,
            scopeId: ScopeId,
            aliasName: AliasName,
            timestamp: Instant = Clock.System.now()
        ): ScopeAlias = ScopeAlias(
            id = id,
            scopeId = scopeId,
            aliasName = aliasName,
            aliasType = AliasType.CANONICAL,
            createdAt = timestamp,
            updatedAt = timestamp
        )
    }

    /**
     * Checks if this alias is canonical.
     */
    fun isCanonical(): Boolean = aliasType == AliasType.CANONICAL

    /**
     * Checks if this alias is custom.
     */
    fun isCustom(): Boolean = aliasType == AliasType.CUSTOM

    /**
     * Creates a copy of this alias with updated timestamp.
     */
    fun withUpdatedTimestamp(timestamp: Instant = Clock.System.now()): ScopeAlias =
        copy(updatedAt = timestamp)

    /**
     * Changes the alias name while preserving the ID and other properties.
     * This allows tracking the same alias entity even when renamed.
     */
    fun withNewName(newName: AliasName, timestamp: Instant = Clock.System.now()): ScopeAlias =
        copy(aliasName = newName, updatedAt = timestamp)
}