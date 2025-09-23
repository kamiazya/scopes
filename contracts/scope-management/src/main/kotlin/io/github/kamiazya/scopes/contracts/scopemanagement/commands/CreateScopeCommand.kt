package io.github.kamiazya.scopes.contracts.scopemanagement.commands

/**
 * Sealed command interface for creating a new scope.
 *
 * This enforces invariants at the type level:
 * - Either auto-generate an alias (WithAutoAlias)
 * - Or provide a custom alias (WithCustomAlias)
 *
 * No invalid states like generateAlias=false with customAlias=null are possible.
 */
public sealed interface CreateScopeCommand {
    public val title: String
    public val description: String?
    public val parentId: String?

    /**
     * Command variant for auto-generating a canonical alias.
     */
    public data class WithAutoAlias(override val title: String, override val description: String? = null, override val parentId: String? = null) :
        CreateScopeCommand

    /**
     * Command variant for providing a custom canonical alias.
     */
    public data class WithCustomAlias(
        override val title: String,
        override val description: String? = null,
        override val parentId: String? = null,
        public val alias: String,
    ) : CreateScopeCommand
}
