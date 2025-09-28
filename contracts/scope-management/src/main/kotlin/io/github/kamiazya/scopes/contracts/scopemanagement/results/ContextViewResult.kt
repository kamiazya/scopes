package io.github.kamiazya.scopes.contracts.scopemanagement.results

import kotlinx.datetime.Instant

/**
 * Result containing context view information.
 */
public data class ContextViewResult(
    /**
     * The unique key of the context view.
     */
    public val key: String,

    /**
     * The display name of the context view.
     */
    public val name: String,

    /**
     * The description of the context view.
     */
    public val description: String?,

    /**
     * The filter expression of the context view.
     */
    public val filter: String,

    /**
     * When the context view was created.
     */
    public val createdAt: Instant,

    /**
     * When the context view was last updated.
     */
    public val updatedAt: Instant,

    /**
     * Whether this is the current active context view.
     */
    public val isActive: Boolean = false,
)
