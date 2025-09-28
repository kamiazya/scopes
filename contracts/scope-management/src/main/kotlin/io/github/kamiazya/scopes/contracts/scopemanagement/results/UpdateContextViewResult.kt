package io.github.kamiazya.scopes.contracts.scopemanagement.results

import kotlinx.datetime.Instant

/**
 * Result of updating a context view.
 */
public data class UpdateContextViewResult(
    /**
     * The key of the updated context view.
     */
    public val key: String,

    /**
     * The updated name (if changed).
     */
    public val name: String,

    /**
     * The updated description (if changed).
     */
    public val description: String?,

    /**
     * The updated filter expression (if changed).
     */
    public val filter: String,

    /**
     * When the context view was updated.
     */
    public val updatedAt: Instant,
)
