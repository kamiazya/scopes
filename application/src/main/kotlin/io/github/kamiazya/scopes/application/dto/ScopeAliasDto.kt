package io.github.kamiazya.scopes.application.dto

import kotlinx.datetime.Instant

/**
 * Data Transfer Object for ScopeAlias entity.
 *
 * Provides a presentation-layer-friendly representation of a scope alias,
 * abstracting away internal implementation details like ULID-based IDs.
 */
data class ScopeAliasResult(
    val scopeId: String,
    val aliasName: String,
    val aliasType: String,
    val createdAt: Instant,
    val updatedAt: Instant
) : DTO
