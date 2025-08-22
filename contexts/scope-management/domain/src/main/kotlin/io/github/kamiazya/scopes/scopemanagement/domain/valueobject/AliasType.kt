package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

/**
 * Enumeration representing the type of scope alias.
 */
enum class AliasType {
    /**
     * The canonical alias - the primary, preferred identifier for the scope.
     * Only one canonical alias exists per scope.
     */
    CANONICAL,

    /**
     * A custom alias - additional identifiers created by users.
     * Multiple custom aliases can exist per scope.
     */
    CUSTOM,
}
