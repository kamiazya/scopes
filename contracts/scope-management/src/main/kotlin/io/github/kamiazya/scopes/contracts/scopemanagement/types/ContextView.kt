package io.github.kamiazya.scopes.contracts.scopemanagement.types

import kotlinx.datetime.Instant

/**
 * Data Transfer Object for ContextView across boundaries.
 */
public data class ContextView(val key: String, val name: String, val filter: String, val description: String?, val createdAt: Instant, val updatedAt: Instant)
