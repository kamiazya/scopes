package io.github.kamiazya.scopes.contracts.scopemanagement.queries

/**
 * Query for listing scopes filtered by aspect criteria.
 *
 * @property aspectKey The aspect key to filter by
 * @property aspectValue The aspect value to match
 * @property parentId Optional parent ID to filter by
 * @property offset Number of items to skip
 * @property limit Maximum number of items to return
 */
public data class ListScopesWithAspectQuery(
    public val aspectKey: String,
    public val aspectValue: String,
    public val parentId: String? = null,
    public val offset: Int = 0,
    public val limit: Int = 20,
)
