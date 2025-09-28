package io.github.kamiazya.scopes.contracts.scopemanagement.results

import kotlinx.datetime.Instant

/**
 * Result of creating a context view.
 */
public data class CreateContextViewResult(
    /**
     * The key of the created context view.
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
)
