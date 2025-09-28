package io.github.kamiazya.scopes.contracts.scopemanagement.results

/**
 * Result of clearing the current context view.
 */
public data class ClearContextViewResult(
    /**
     * The key of the context view that was cleared.
     */
    public val clearedKey: String?,
)
