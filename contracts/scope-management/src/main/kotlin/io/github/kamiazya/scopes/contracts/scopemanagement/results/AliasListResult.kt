package io.github.kamiazya.scopes.contracts.scopemanagement.results

/**
 * Result containing a list of aliases for a scope.
 *
 * This result DTO provides comprehensive alias information for a specific scope,
 * including both canonical and custom aliases sorted appropriately.
 */
public data class AliasListResult(public val scopeId: String, public val aliases: List<AliasInfo>, public val totalCount: Int)
