package io.github.kamiazya.scopes.application.dto

/**
 * Result DTO for alias removal.
 *
 * Contains information about the removed alias.
 * The presence of this result in Either.Right already indicates success,
 * so we return useful information about what was removed.
 */
data class RemoveAliasResult(
    /**
     * The ID of the removed alias
     */
    val aliasId: String,
    
    /**
     * The name of the removed alias
     */
    val aliasName: String,
    
    /**
     * The scope ID that this alias was pointing to
     */
    val scopeId: String,
    
    /**
     * Whether this was a canonical alias (should always be false for removals)
     */
    val wasCanonical: Boolean = false
) : DTO
