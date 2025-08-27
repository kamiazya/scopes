package io.github.kamiazya.scopes.scopemanagement.application.dto

import kotlinx.datetime.Instant

/**
 * DTO representing information about a single alias.
 *
 * @property aliasName The name of the alias
 * @property aliasType The type of the alias (CANONICAL or CUSTOM)
 * @property isCanonical Whether this is the canonical alias
 * @property createdAt Creation timestamp
 */
data class AliasInfoDto(val aliasName: String, val aliasType: String, val isCanonical: Boolean, val createdAt: Instant) : DTO
