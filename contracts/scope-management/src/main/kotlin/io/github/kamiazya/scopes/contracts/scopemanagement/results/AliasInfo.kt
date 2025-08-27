package io.github.kamiazya.scopes.contracts.scopemanagement.results

import kotlinx.datetime.Instant

/**
 * Information about a single alias.
 *
 * This DTO represents alias details for external clients,
 * providing both the alias name and metadata about its type and creation.
 */
public data class AliasInfo(public val aliasName: String, public val aliasType: String, public val isCanonical: Boolean, public val createdAt: Instant)
