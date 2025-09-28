package io.github.kamiazya.scopes.contracts.scopemanagement.results

/**
 * Result of switching to a different context view.
 */
public data class SwitchContextViewResult(
    /**
     * The key of the previously active context view (if any).
     */
    public val previousKey: String?,

    /**
     * The key of the newly activated context view.
     */
    public val newKey: String,

    /**
     * The filter expression of the newly activated context view.
     */
    public val filter: String,
)
