package io.github.kamiazya.scopes.application.dto

/**
 * Result DTO for alias resolution.
 *
 * Returns the scope ID that the alias points to.
 */
data class ResolveAliasResult(val scopeId: String) : DTO
